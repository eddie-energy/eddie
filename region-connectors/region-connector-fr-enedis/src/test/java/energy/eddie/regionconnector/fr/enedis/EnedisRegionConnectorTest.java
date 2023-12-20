package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisInvalidState;
import energy.eddie.regionconnector.fr.enedis.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisRegionConnectorTest {
    @Test
    void health_returnsHealthChecks() {
        // Given
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
        try (var rc = new EnedisRegionConnector(enedisApi, mock(PermissionRequestService.class), sink, consumptionRecordSink)) {

            // When
            var res = rc.health();

            // Then
            assertEquals(Map.of("service", HealthState.UP), res);
        }
    }

    @Test
    void getMetadata_returnsExpected() {
        // Given
        var enedisApi = mock(EnedisApi.class);
        var permissionRequestService = mock(PermissionRequestService.class);
        Sinks.Many<ConnectionStatusMessage> connectionStatusSink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
        try (var rc = new EnedisRegionConnector(enedisApi, permissionRequestService, connectionStatusSink, consumptionRecordSink)) {

            // When
            var res = rc.getMetadata();

            // Then
            assertEquals(EnedisRegionConnectorMetadata.getInstance(), res);
        }
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findPermissionRequestByPermissionId(anyString())).thenReturn(Optional.empty());
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
        try (var rc = new EnedisRegionConnector(enedisApi, permissionRequestService, sink, consumptionRecordSink)) {

            // When
            // Then
            assertDoesNotThrow(() -> rc.terminatePermission("pid"));
        }
    }

    @Test
    void terminatePermission_withExistingPermissionId_throws() {
        // Given
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new FrEnedisAcceptedState(null)
        );
        when(permissionRequestService.findPermissionRequestByPermissionId(anyString()))
                .thenReturn(Optional.of(request));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
        try (var rc = new EnedisRegionConnector(enedisApi, permissionRequestService, sink, consumptionRecordSink)) {

            // When
            // Then
            assertThrows(UnsupportedOperationException.class, () -> rc.terminatePermission("pid"));
        }
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new FrEnedisInvalidState(null)
        );
        when(permissionRequestService.findPermissionRequestByPermissionId(anyString())).thenReturn(Optional.of(request));
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        Sinks.Many<ConsumptionRecord> consumptionRecordSink = Sinks.many().multicast().onBackpressureBuffer();
        try (var rc = new EnedisRegionConnector(enedisApi, permissionRequestService, sink, consumptionRecordSink)) {

            // When
            // Then
            assertDoesNotThrow(() -> rc.terminatePermission("pid"));
        }
    }
}