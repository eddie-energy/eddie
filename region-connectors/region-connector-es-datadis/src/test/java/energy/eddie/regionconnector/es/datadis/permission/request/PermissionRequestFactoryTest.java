package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.es.datadis.api.AuthorizationApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;
import java.util.UUID;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PermissionRequestFactoryTest {
    @Mock
    private AuthorizationApi unusedAuthorizationApi;
    @Mock
    private Sinks.Many<ConnectionStatusMessage> unusedConnectionStatusMessageSink;
    @Mock
    private EsPermissionRequestRepository unusedRepository;
    @InjectMocks
    private PermissionRequestFactory factory;

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
        assertEquals(requestDataFrom, createdRequest.requestDataFrom());
        assertEquals(requestDataTo, createdRequest.requestDataTo());
        assertTrue(createdRequest.distributorCode().isEmpty());
        assertTrue(createdRequest.lastPulledMeterReading().isEmpty());
        assertTrue(createdRequest.pointType().isEmpty());
    }
}