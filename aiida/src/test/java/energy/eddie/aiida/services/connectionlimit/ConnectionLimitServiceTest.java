// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionLimitServiceTest {
    private static final UUID USER_ID = UUID.fromString("092bf5cb-8571-4313-9429-8caf5e679f6e");

    @Mock
    private ConnectionLimitRepository connectionLimitRepository;
    @Mock
    private AuthService authService;

    private ConnectionLimitService service;

    @BeforeEach
    void setUp() {
        var fixedClock = Clock.fixed(Instant.parse("2026-07-10T08:45:50Z"), ZoneOffset.UTC);
        service = new ConnectionLimitService(connectionLimitRepository, authService, fixedClock);
    }

    @Test
    void givenNoQueryParams_defaultsFromAndToToNow() throws Exception {
        var now = Instant.parse("2026-07-10T08:45:50Z");
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(connectionLimitRepository.findEffectiveByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                             isNull(),
                                                                             isNull(),
                                                                             eq(now),
                                                                             eq(now),
                                                                             isNull())).thenReturn(List.of());

        service.getConnectionLimits(null, null, null, null, null);

        verify(connectionLimitRepository).findEffectiveByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                                isNull(),
                                                                                isNull(),
                                                                                eq(now),
                                                                                eq(now),
                                                                                isNull());
    }

    @Test
    void givenOffset_usesPageSizeOffsetPlusOne() throws Exception {
        var from = Instant.parse("2026-07-10T10:00:00Z");
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(connectionLimitRepository.findEffectiveByUserIdAndFiltersFromTo(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.getConnectionLimits(null, null, from, null, 2);

        verify(connectionLimitRepository).findEffectiveByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                                isNull(),
                                                                                isNull(),
                                                                                eq(from),
                                                                                isNull(),
                                                                                eq(3));
    }

    @Test
    void givenFromAfterTo_returnsEmptyWithoutRepositoryCall() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);

        var result = service.getConnectionLimits(null,
                                                 null,
                                                 Instant.parse("2026-07-10T09:00:00Z"),
                                                 Instant.parse("2026-07-10T08:00:00Z"),
                                                 null);

        assertTrue(result.isEmpty());
        verify(connectionLimitRepository, never()).findEffectiveByUserIdAndFiltersFromTo(any(),
                                                                                         any(),
                                                                                         any(),
                                                                                         any(),
                                                                                         any(),
                                                                                         any());
    }

    @Test
    void givenOverlappingIntervals_returnsEffectiveTimelineWithNewestCreatedAt() throws Exception {
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(connectionLimitRepository.findEffectiveByUserIdAndFiltersFromTo(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(limit("2026-07-10T10:00:00Z", "2026-07-10T10:10:00Z", "2.0", "8.0"),
                                    limit("2026-07-10T10:10:00Z", "2026-07-10T10:25:00Z", "3.0", "9.0"),
                                    limit("2026-07-10T10:25:00Z", "2026-07-10T10:30:00Z", "1.0", "7.0")));

        var result = service.getConnectionLimits(null, null,
                                                 Instant.parse("2026-07-10T10:00:00Z"),
                                                 Instant.parse("2026-07-10T10:30:00Z"), null);

        assertEquals(3, result.size());
        assertEquals(Instant.parse("2026-07-10T10:00:00Z"), result.get(0).intervalStart());
        assertEquals(new BigDecimal("2.0"), result.get(0).minLimitKw());
        assertEquals(Instant.parse("2026-07-10T10:10:00Z"), result.get(1).intervalStart());
        assertEquals(new BigDecimal("3.0"), result.get(1).minLimitKw());
        assertEquals(Instant.parse("2026-07-10T10:25:00Z"), result.get(2).intervalStart());
        assertEquals(new BigDecimal("1.0"), result.get(2).minLimitKw());
    }

    private static ConnectionLimitRepository.EffectiveConnectionLimitProjection limit(
            String intervalStart, String intervalEnd, String minLimitKw, String maxLimitKw) {
        return new EffectiveLimitProjection(UUID.fromString("9921f327-f341-4bea-bf08-3cf2acc65bf3"),
                                            "003114735",
                                            Instant.parse(intervalStart), Instant.parse(intervalEnd),
                                            new BigDecimal(minLimitKw), new BigDecimal(maxLimitKw), null);
    }

    private record EffectiveLimitProjection(
            UUID permissionId, String meterId, Instant intervalStart, Instant intervalEnd,
            BigDecimal minLimitKw, BigDecimal maxLimitKw, @jakarta.annotation.Nullable String mrid
    ) implements ConnectionLimitRepository.EffectiveConnectionLimitProjection {
        @Override public UUID getPermissionId() { return permissionId; }
        @Override public String getMeterId() { return meterId; }
        @Override public Instant getIntervalStart() { return intervalStart; }
        @Override public Instant getIntervalEnd() { return intervalEnd; }
        @Override public BigDecimal getMinLimitKw() { return minLimitKw; }
        @Override public BigDecimal getMaxLimitKw() { return maxLimitKw; }
        @Override public @jakarta.annotation.Nullable String getMrid() { return mrid; }
    }
}
