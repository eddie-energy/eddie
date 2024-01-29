package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import jakarta.xml.bind.JAXBException;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EdaRegionConnectorTest {

    @Test
    void connectorThrows_ifEdaAdapterNull() {
        // given
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(null, requestService, sink));
    }

    @Test
    void connectorThrows_ifPermissionRequestRepoNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, null, sink));
    }

    @Test
    void connectorThrows_ifConsumptionRecordProcessorNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, requestService, sink));
    }

    @Test
    void connectorThrows_ifConnectionStatusMessageSinkNull() {
        // given
        var adapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);

        // when
        // then
        assertThrows(NullPointerException.class, () -> new EdaRegionConnector(adapter, requestService, null));
    }

    @Test
    void connectorConstructs() {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        // when
        // then
        assertDoesNotThrow(() -> new EdaRegionConnector(adapter, requestService, sink));
    }

    @Test
    void terminateNonExistingPermission_doesNothing() throws TransmissionException, JAXBException {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        when(requestService.findByPermissionId("permissionId")).thenReturn(Optional.empty());
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connector = new EdaRegionConnector(adapter, requestService, sink);

        // when
        // then
        assertDoesNotThrow(() -> connector.terminatePermission("permissionId"));

        verify(adapter, never()).sendCMRevoke(any());
    }

    @Test
    void getMetadata_returnExpectedMetadata() throws TransmissionException {
        // given
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var connector = new EdaRegionConnector(adapter, requestService, sink);

        // when
        var result = connector.getMetadata();

        // then
        assertEquals(EdaRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void close_ClosesRelatedResources() throws Exception {
        var adapter = mock(EdaAdapter.class);
        when(adapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var connector = new EdaRegionConnector(adapter, requestService, sink);

        connector.close();

        verify(adapter).close();
    }

    @Test
    void health_returnsHealthChecks() throws TransmissionException {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        var requestService = mock(PermissionRequestService.class);
        when(edaAdapter.health()).thenReturn(Map.of("service", HealthState.UP));
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();

        var rc = new EdaRegionConnector(edaAdapter, requestService, sink);

        // When
        var res = rc.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void close_emitsCompleteOnPublisherForConnectionStatusMessages() throws Exception {
        // Given
        var edaAdapter = mock(EdaAdapter.class);
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(Flux.empty());
        var requestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        var rc = new EdaRegionConnector(edaAdapter, requestService, sink);
        StepVerifier stepVerifier = StepVerifier.create(JdkFlowAdapter.flowPublisherToFlux(rc.getConnectionStatusMessageStream()))
                .expectComplete()
                .verifyLater();

        // When
        rc.close();

        // Then
        stepVerifier.verify(Duration.ofSeconds(2));
    }
}