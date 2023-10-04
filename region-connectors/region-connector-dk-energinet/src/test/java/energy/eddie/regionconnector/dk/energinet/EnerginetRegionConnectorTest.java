package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.config.EnerginetConfiguration;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerAcceptedState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerInvalidState;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnerginetRegionConnectorTest {
    @Test
    void health_returnsHealthChecks() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("customerPath");
        when(config.thirdpartyBasePath()).thenReturn("thirdpartyPath");
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        var rc = new EnerginetRegionConnector(config, energinetCustomerApi, new InMemoryPermissionRequestRepository());

        // When
        var res = rc.health();
        assertEquals(Map.of("service", HealthState.UP), res);
    }


    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("customerPath");
        when(config.thirdpartyBasePath()).thenReturn("thirdpartyPath");
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        var rc = new EnerginetRegionConnector(config, energinetCustomerApi, permissionRequestRepository);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withExistingPermissionId_throws() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("customerPath");
        when(config.thirdpartyBasePath()).thenReturn("thirdpartyPath");
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerAcceptedState(null)
        );
        permissionRequestRepository.save(request);
        var rc = new EnerginetRegionConnector(config, energinetCustomerApi, permissionRequestRepository);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, () -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var config = mock(EnerginetConfiguration.class);
        when(config.customerBasePath()).thenReturn("customerPath");
        when(config.thirdpartyBasePath()).thenReturn("thirdpartyPath");
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<DkEnerginetCustomerPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerInvalidState(null)
        );
        permissionRequestRepository.save(request);
        var rc = new EnerginetRegionConnector(config, energinetCustomerApi, permissionRequestRepository);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }
}
