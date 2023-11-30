package energy.eddie.regionconnector.dk.energinet;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.HealthState;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerAcceptedState;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerInvalidState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnerginetRegionConnectorTest {
    private Sinks.Many<ConnectionStatusMessage> statusSink;
    private Sinks.Many<ConsumptionRecord> recordSink;

    @BeforeEach
    void setUp() {
        statusSink = Sinks.many().unicast().onBackpressureBuffer();
        recordSink = Sinks.many().unicast().onBackpressureBuffer();
    }

    @Test
    void health_returnsHealthChecks() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        var rc = new EnerginetRegionConnector(statusSink, recordSink, energinetCustomerApi, new InMemoryPermissionRequestRepository(), 0);

        // When
        var res = rc.health();
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void getMetadata_returnsExpected() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        var rc = new EnerginetRegionConnector(statusSink, recordSink, energinetCustomerApi, new InMemoryPermissionRequestRepository(), 0);

        // When
        var result = rc.getMetadata();

        assertEquals(EnerginetRegionConnectorMetadata.getInstance(), result);
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        var rc = new EnerginetRegionConnector(statusSink, recordSink, energinetCustomerApi, permissionRequestRepository, 0);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withExistingPermissionId_throws() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dataNeedId",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerAcceptedState(null)
        );
        permissionRequestRepository.save(request);
        var rc = new EnerginetRegionConnector(statusSink, recordSink, energinetCustomerApi, permissionRequestRepository, 0);

        // When
        // Then
        assertThrows(UnsupportedOperationException.class, () -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var energinetCustomerApi = mock(EnerginetCustomerApiClient.class);
        when(energinetCustomerApi.health()).thenReturn(Map.of("service", HealthState.UP));
        DkEnerginetCustomerPermissionRequestRepository permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dataNeedId",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new EnerginetCustomerInvalidState(null)
        );
        permissionRequestRepository.save(request);
        var rc = new EnerginetRegionConnector(statusSink, recordSink, energinetCustomerApi, permissionRequestRepository, 0);

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }
}