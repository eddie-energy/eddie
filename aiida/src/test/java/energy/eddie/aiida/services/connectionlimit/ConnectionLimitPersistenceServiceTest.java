// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.models.connectionlimit.ConnectionLimit;
import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitPersistenceServiceTest {

    private static final UUID PERMISSION_ID = UUID.fromString("00213495-bdbf-4497-8695-5d811e45aa64");
    private static final String METER_ID = "003114735";
    private static final String MRID = "document-1";
    private static final Instant INCOMING_CREATED_AT = Instant.parse("2026-02-16T10:11:58Z");

    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Captor
    private ArgumentCaptor<List<ConnectionLimit>> limitsCaptor;
    private ConnectionLimitPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new ConnectionLimitPersistenceService(connectionLimitRepository, transactionTemplate);
    }

    @Test
    void replaceByMrid_deletesAndSavesAllLimits() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        service.replaceByMrid(MRID, connectionLimits());

        verify(connectionLimitRepository).deleteByMrid(MRID);
        verify(connectionLimitRepository).saveAll(limitsCaptor.capture());
        assertEquals(2, limitsCaptor.getValue().size());
        verify(connectionLimitRepository, never()).save(any(ConnectionLimit.class));
    }

    @Test
    void upsertByCreatedAt_persistsPointsWithoutExistingValues() {
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        var persisted = service.upsertByCreatedAt(connectionLimits());

        verify(connectionLimitRepository, times(2)).save(any(ConnectionLimit.class));
        assertEquals(2, persisted);
    }

    @Test
    void upsertByCreatedAt_returnsOnlyPersistedCount() {
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:00:00Z"),
                                                                                 Instant.parse("2026-06-01T00:15:00Z")))
                .thenReturn(Optional.of(Instant.parse("2026-02-16T11:11:58Z")));
        when(connectionLimitRepository.findCreatedAtByPermissionMeterAndInterval(PERMISSION_ID,
                                                                                 METER_ID,
                                                                                 Instant.parse("2026-06-01T00:15:00Z"),
                                                                                 Instant.parse("2026-06-01T00:30:00Z")))
                .thenReturn(Optional.empty());

        var persisted = service.upsertByCreatedAt(connectionLimits());

        verify(connectionLimitRepository).save(any(ConnectionLimit.class));
        assertEquals(1, persisted);
    }

    private static List<ConnectionLimit> connectionLimits() {
        return List.of(connectionLimit(Instant.parse("2026-06-01T00:00:00Z")),
                       connectionLimit(Instant.parse("2026-06-01T00:15:00Z"))
        );
    }

    private static ConnectionLimit connectionLimit(Instant intervalStart) {
        return new ConnectionLimit(PERMISSION_ID,
                                   METER_ID,
                                   intervalStart,
                                   intervalStart.plus(Duration.ofMinutes(15)),
                                   new BigDecimal("2.0"),
                                   new BigDecimal("8.0"),
                                   MRID,
                                   1,
                                   INCOMING_CREATED_AT);
    }
}
