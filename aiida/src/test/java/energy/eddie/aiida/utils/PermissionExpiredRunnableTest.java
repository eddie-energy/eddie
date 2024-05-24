package energy.eddie.aiida.utils;

import energy.eddie.aiida.config.AiidaConfiguration;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionExpiredRunnableTest {
    @Mock
    private Permission permission;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionRepository repository;
    private final Instant fixedInstant = Instant.parse("2024-05-01T23:59:59.00Z");
    private final Clock clock = Clock.fixed(fixedInstant, AiidaConfiguration.AIIDA_ZONE_ID);

    @BeforeEach
    void setUp() {
    }

    @Test
    void verify_run_changesState_stopsStreamer_andUpdatesDb() {
        // Given
        when(permission.expirationTime()).thenReturn(fixedInstant);
        when(permission.status()).thenReturn(PermissionStatus.STREAMING_DATA);
        var runnable = new PermissionExpiredRunnable(permission, streamerManager, repository, clock);

        // When
        runnable.run();

        // Then
        verify(streamerManager).permissionExpired(any());
        verify(permission).setStatus(PermissionStatus.FULFILLED);
        verify(repository).save(any());
    }

    @Test
    void givenPermissionInInvalidState_run_doesNotExpirePermission() {
        // Given
        when(permission.expirationTime()).thenReturn(fixedInstant.minusSeconds(1));
        when(permission.status()).thenReturn(PermissionStatus.REVOKED);
        var runnable = new PermissionExpiredRunnable(permission, streamerManager, repository, clock);

        // When
        runnable.run();

        // Then
        verifyNoInteractions(streamerManager);
        verifyNoInteractions(repository);
        verify(permission, never()).setStatus(any());
    }
}
