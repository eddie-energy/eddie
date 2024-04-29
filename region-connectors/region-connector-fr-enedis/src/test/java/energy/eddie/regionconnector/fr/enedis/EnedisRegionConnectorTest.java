package energy.eddie.regionconnector.fr.enedis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.HealthState;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnedisRegionConnectorTest {
    @Mock
    private FrPermissionRequestRepository repository;
    @Mock
    private EnedisApi enedisApi;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private EnedisRegionConnector rc;

    @Test
    void health_returnsHealthChecks() {
        // Given
        when(enedisApi.health()).thenReturn(Map.of("service", HealthState.UP));

        // When
        var res = rc.health();

        // Then
        assertEquals(Map.of("service", HealthState.UP), res);
    }

    @Test
    void getMetadata_returnsExpected() {
        // Given
        // When
        var res = rc.getMetadata();

        // Then
        assertEquals(EnedisRegionConnectorMetadata.getInstance(), res);
    }

    @Test
    void terminatePermission_withNonExistentPermissionId_doesNotThrow() {
        // Given
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.empty());

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }

    @Test
    void terminatePermission_withExistingPermissionId_terminates() {
        // Given
        var request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.now(ZoneOffset.UTC),
                LocalDate.now(ZoneOffset.UTC),
                Granularity.P1D,
                PermissionProcessStatus.ACCEPTED
        );
        when(repository.findByPermissionId(anyString()))
                .thenReturn(Optional.of(request));

        // When
        rc.terminatePermission("pid");
        // Then
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.TERMINATED, event.status())));
    }

    @Test
    void terminatePermission_withWrongState_doesNotThrow() {
        // Given
        var request = new EnedisPermissionRequest(
                "pid",
                "cid",
                "dnid",
                LocalDate.now(Clock.systemUTC()),
                LocalDate.now(Clock.systemUTC()),
                Granularity.P1D,
                PermissionProcessStatus.CREATED
        );
        when(repository.findByPermissionId(anyString())).thenReturn(Optional.of(request));

        // When
        // Then
        assertDoesNotThrow(() -> rc.terminatePermission("pid"));
    }
}
