package energy.eddie.regionconnector.dk.energinet;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.states.TerminatedPermissionRequestState;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.DkEnerginetSpringConfig;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.permission.request.EnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.permission.request.StateBuilderFactory;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerAcceptedState;
import energy.eddie.regionconnector.dk.energinet.permission.request.states.EnerginetCustomerInvalidState;
import energy.eddie.regionconnector.dk.energinet.services.PermissionRequestService;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
    void terminatePermission_withExistingPermissionId_terminates() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        LocalDate start = LocalDate.now(ZoneOffset.UTC);
        LocalDate end = start.plusDays(10);
        var creation = new PermissionRequestForCreation("cid", "token", "mpid", "dnid");
        StateBuilderFactory factory = new StateBuilderFactory();
        ObjectMapper mapper = new DkEnerginetSpringConfig().objectMapper();
        var permissionRequest = new EnerginetCustomerPermissionRequest("pid",
                                                                       creation,
                                                                       mock(EnerginetCustomerApi.class),
                                                                       start,
                                                                       end,
                                                                       Granularity.PT15M,
                                                                       factory, mapper);
        EnerginetCustomerAcceptedState state = new EnerginetCustomerAcceptedState(permissionRequest, factory);
        permissionRequest.changeState(state);
        PermissionRequestService permissionRequestService = mock(PermissionRequestService.class);
        when(permissionRequestService.findByPermissionId(anyString())).thenReturn(Optional.of(permissionRequest));
        var rc = new EnerginetRegionConnector(energinetCustomerApi, permissionRequestService);

        // When
        rc.terminatePermission("pid");

        // Then
        assertInstanceOf(TerminatedPermissionRequestState.class, permissionRequest.state());
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
                LocalDate.now(Clock.systemUTC()),
                LocalDate.now(Clock.systemUTC()),
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
