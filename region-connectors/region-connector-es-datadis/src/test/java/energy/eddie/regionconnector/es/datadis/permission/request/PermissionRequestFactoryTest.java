package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.permission.requests.extensions.Extension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFactoryTest {
    @Mock
    private Sinks.Many<ConnectionStatusMessage> unusedConnectionStatusMessageSink;
    @Mock
    private AuthorizationApi authorizationApi;
    @Mock
    private Set<Extension<EsPermissionRequest>> unusedExtensions;
    private PermissionRequestFactory factory;

    @BeforeEach
    void setUp() {
        StateBuilderFactory stateFactory = new StateBuilderFactory(authorizationApi);
        factory = new PermissionRequestFactory(unusedConnectionStatusMessageSink, unusedExtensions, stateFactory);
    }

    @Test
    void givenValidInput_returnsPermissionRequestWithSetId() {
        // Given
        String meteringPointId = "7890";
        String nif = "123456";
        String dataNeedId = "dataNeed";
        String connectionId = "connId";
        MeasurementType measurementType = MeasurementType.QUARTER_HOURLY;
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);
        ZonedDateTime requestDataFrom = now.minusDays(10);
        ZonedDateTime requestDataTo = now.minusDays(5);
        var requestForCreation = new PermissionRequestForCreation(connectionId, dataNeedId, nif,
                meteringPointId, requestDataFrom, requestDataTo, measurementType);

        // When
        EsPermissionRequest createdRequest = factory.create(requestForCreation);

        // Then
        assertDoesNotThrow(() -> UUID.fromString(createdRequest.permissionId()));
        assertEquals(connectionId, createdRequest.connectionId());
        assertEquals(dataNeedId, createdRequest.dataNeedId());
        assertEquals(nif, createdRequest.nif());
        assertEquals(meteringPointId, createdRequest.meteringPointId());
        assertEquals(measurementType, createdRequest.measurementType());
        assertEquals(requestDataFrom, createdRequest.start());
        assertEquals(requestDataTo.plusDays(1), createdRequest.end());
        assertTrue(createdRequest.distributorCode().isEmpty());
        assertTrue(createdRequest.lastPulledMeterReading().isEmpty());
        assertTrue(createdRequest.pointType().isEmpty());
    }

    @Test
    void close_emitsCompleteOnPublisher() {
        // Given
        var factory = new PermissionRequestFactory(Sinks.many().multicast().onBackpressureBuffer(), Set.of(), new StateBuilderFactory(authorizationApi));
        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(factory.getConnectionStatusMessageStream()))
                .expectComplete()
                .verifyLater();

        // When
        factory.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}