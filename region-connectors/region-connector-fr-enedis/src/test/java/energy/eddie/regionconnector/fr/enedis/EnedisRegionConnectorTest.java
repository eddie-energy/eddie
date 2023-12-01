package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.InMemoryPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.permission.request.SimplePermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisAcceptedState;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisInvalidState;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnedisRegionConnectorTest {
    @Test
    void health_returnsHealthChecks() {
        // Given
        var config = mock(EnedisConfiguration.class);
        when(config.clientId()).thenReturn("id");
        when(config.clientSecret()).thenReturn("secret");
        when(config.basePath()).thenReturn("path");
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        try (var rc = new EnedisRegionConnector(config, enedisApi, new InMemoryPermissionRequestRepository())) {

            // When
            var res = rc.health();

            // Then
            assertEquals(Map.of("service", HealthState.UP), res);
        }
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        var config = mock(EnedisConfiguration.class);
        when(config.clientId()).thenReturn("id");
        when(config.clientSecret()).thenReturn("secret");
        when(config.basePath()).thenReturn("path");
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        try (var rc = new EnedisRegionConnector(config, enedisApi, permissionRequestRepository)) {

            // When
            // Then
            assertDoesNotThrow(() -> rc.terminatePermission("pid"));
        }
    }

    @Test
    void terminatePermission_withExistingPermissionId_throws() {
        // Given
        var config = mock(EnedisConfiguration.class);
        when(config.clientId()).thenReturn("id");
        when(config.clientSecret()).thenReturn("secret");
        when(config.basePath()).thenReturn("path");
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new FrEnedisAcceptedState(null)
        );
        permissionRequestRepository.save(request);
        try (var rc = new EnedisRegionConnector(config, enedisApi, permissionRequestRepository)) {

            // When
            // Then
            assertThrows(UnsupportedOperationException.class, () -> rc.terminatePermission("pid"));
        }
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var config = mock(EnedisConfiguration.class);
        when(config.clientId()).thenReturn("id");
        when(config.clientSecret()).thenReturn("secret");
        when(config.basePath()).thenReturn("path");
        var enedisApi = mock(EnedisApi.class);
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));
        PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository = new InMemoryPermissionRequestRepository();
        SimplePermissionRequest request = new SimplePermissionRequest(
                "pid",
                "cid",
                "dnid",
                ZonedDateTime.now(Clock.systemUTC()),
                ZonedDateTime.now(Clock.systemUTC()),
                new FrEnedisInvalidState(null)
        );
        permissionRequestRepository.save(request);
        try (var rc = new EnedisRegionConnector(config, enedisApi, permissionRequestRepository)) {

            // When
            // Then
            assertDoesNotThrow(() -> rc.terminatePermission("pid"));
        }
    }
}