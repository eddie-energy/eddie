package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerAcceptedState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerInvalidState;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnerginetRegionConnectorTest {
    @Test
    void health_returnsHealthChecks() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Mono.just(Map.of("service", HealthState.UP)));
        var rc = new EnerginetRegionConnector(energinetCustomerApi, mock(PermissionRequestService.class));

        // When
        var res = rc.health();
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void getMetadata_returnsExpected() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        var rc = new EnerginetRegionConnector(energinetCustomerApi, mock(PermissionRequestService.class));

        // When
        var result = rc.getMetadata();

        assertEquals(EnerginetRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Mono.just(Map.of("service", HealthState.UP)));
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByPermissionId(anyString())).thenReturn(Optional.empty());
        var rc = new EnerginetRegionConnector(energinetCustomerApi, permissionRequestService);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withExistingPermissionId_throws() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Mono.just(Map.of("service", HealthState.UP)));
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dataNeedId",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerAcceptedState(null)
        );
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByPermissionId(anyString())).thenReturn(Optional.of(request));
        var rc = new EnerginetRegionConnector(energinetCustomerApi, permissionRequestService);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, () -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Mono.just(Map.of("service", HealthState.UP)));
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dataNeedId",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerInvalidState(null)
        );
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByPermissionId(anyString())).thenReturn(Optional.of(request));
        var rc = new EnerginetRegionConnector(energinetCustomerApi, permissionRequestService);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }
}