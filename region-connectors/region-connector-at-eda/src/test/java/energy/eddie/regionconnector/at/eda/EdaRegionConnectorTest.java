package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.config.PlainAtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdaRegionConnectorTest {
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;

    @Test
    void connectorConstructs() {
        // given
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(edaAdapter, repository, sink, outbox, null));
    }

    @Test
    void terminateNonExistingPermission_doesNothing() throws TransmissionException, JAXBException {
        // given
        when(repository.findByPermissionId("permissionId")).thenReturn(Optional.empty());
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, null, null);

        // when
        // then
        assertDoesNotThrow(() -> regionConnector.terminatePermission("permissionId"));

        verify(edaAdapter, never()).sendCMRevoke(any());
    }

    @Test
    void terminateExistingPermission_sendsCmRevoke() throws TransmissionException, JAXBException {
        // given
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, "",
                                                         "consentId", start);
        when(repository.findByPermissionId("permissionId")).thenReturn(Optional.of(permissionRequest));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var epId = new PlainAtConfiguration("epId", null);
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, epId);

        // when
        regionConnector.terminatePermission("permissionId");

        // then
        verify(edaAdapter).sendCMRevoke(any());
        verify(outbox).commit(any());
    }

    @Test
    void terminatePermission_edaThrows_terminatesOnEPSide() throws TransmissionException, JAXBException {
        // given
        var start = ZonedDateTime.now(ZoneOffset.UTC);
        var end = start.plusDays(10);
        doThrow(new TransmissionException(null)).when(edaAdapter).sendCMRevoke(any());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", "mid", "dsoId", start, end,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.ACCEPTED, "",
                                                         "consentId", start);
        when(repository.findByPermissionId("permissionId")).thenReturn(Optional.of(permissionRequest));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var epId = new PlainAtConfiguration("epId", null);
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, epId);

        // when
        regionConnector.terminatePermission("permissionId");

        // then
        verify(outbox).commit(any());
    }

    @Test
    void getMetadata_returnExpectedMetadata() throws TransmissionException {
        // given
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, null);

        // when
        var result = regionConnector.getMetadata();

        // then
        assertEquals(EdaRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void close_ClosesRelatedResources() throws Exception {
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, null);

        regionConnector.close();

        verify(edaAdapter).close();
    }

    @Test
    void health_returnsHealthChecks() throws TransmissionException {
        // Given
        when(edaAdapter.health()).thenReturn(Map.of("service", HealthState.UP));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, null);

        // When
        var res = regionConnector.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void close_emitsCompleteOnPublisherForConnectionStatusMessages() throws Exception {
        // Given
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var regionConnector = new EdaRegionConnector(edaAdapter, repository, sink, outbox, null);
        StepVerifier stepVerifier = StepVerifier.create(regionConnector.getConnectionStatusMessageStream())
                .expectComplete()
                .verifyLater();

        // When
        regionConnector.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}