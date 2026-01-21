package energy.eddie.aiida.services.cleanup;

import energy.eddie.aiida.config.cleanup.CleanupEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeBasedCleanupServiceTest {
    @Mock
    ExpiredEntityDeleter expiredEntityDeleter;

    TimeBasedCleanupService service;

    @BeforeEach
    void setUp() {
        service = new TimeBasedCleanupService(
                CleanupEntity.AIIDA_RECORD,
                Duration.ofDays(30),
                expiredEntityDeleter
        ) {};
    }

    @Test
    void deleteExpiredEntities_deletesUntilNoMoreEntitiesExist() {
        // Given
        when(expiredEntityDeleter.deleteOldestByTimestampBefore(any(), eq(1000)))
                .thenReturn(1000)
                .thenReturn(500)
                .thenReturn(0);

        // When
        var deleted = service.deleteExpiredEntities();

        // Then
        assertEquals(1500, deleted);
        verify(expiredEntityDeleter, times(3))
                .deleteOldestByTimestampBefore(any(), eq(1000));
    }

    @Test
    void deleteExpiredEntities_deletesUntilException() {
        // given
        when(expiredEntityDeleter.deleteOldestByTimestampBefore(any(), eq(1000)))
                .thenReturn(1000)
                .thenThrow(new RuntimeException("DB error"));

        // when
        var deleted = service.deleteExpiredEntities();

        // then
        assertEquals(1000, deleted);
        verify(expiredEntityDeleter, times(2))
                .deleteOldestByTimestampBefore(any(), eq(1000));
    }

    @Test
    void deleteExpiredEntities_returnsZero_whenNothingDeleted() {
        // given
        when(expiredEntityDeleter.deleteOldestByTimestampBefore(any(), eq(1000)))
                .thenReturn(0);

        // when
        var deleted = service.deleteExpiredEntities();

        // then
        assertEquals(0, deleted);
        verify(expiredEntityDeleter, times(1))
                .deleteOldestByTimestampBefore(any(), eq(1000));
    }
}
