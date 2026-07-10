// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services.connectionlimit;

import energy.eddie.aiida.repositories.ConnectionLimitRepository;
import energy.eddie.aiida.services.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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
        when(connectionLimitRepository.findByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                    isNull(),
                                                                    isNull(),
                                                                    eq(now),
                                                                    eq(now),
                                                                    any())).thenReturn(List.of());

        service.getConnectionLimits(null, null, null, null, null);

        verify(connectionLimitRepository).findByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                       isNull(),
                                                                       isNull(),
                                                                       eq(now),
                                                                       eq(now),
                                                                       any());
    }

    @Test
    void givenOffset_usesPageSizeOffsetPlusOne() throws Exception {
        var from = Instant.parse("2026-07-10T10:00:00Z");
        when(authService.getCurrentUserId()).thenReturn(USER_ID);
        when(connectionLimitRepository.findByUserIdAndFiltersFromTo(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        service.getConnectionLimits(null, null, from, null, 2);

        var pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        var toCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(connectionLimitRepository).findByUserIdAndFiltersFromTo(eq(USER_ID),
                                                                       isNull(),
                                                                       isNull(),
                                                                       eq(from),
                                                                       toCaptor.capture(),
                                                                       pageableCaptor.capture());
        assertNull(toCaptor.getValue());
        assertEquals(3, pageableCaptor.getValue().getPageSize());
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
        verify(connectionLimitRepository, never()).findByUserIdAndFiltersFromTo(any(),
                                                                                any(),
                                                                                any(),
                                                                                any(),
                                                                                any(),
                                                                                any());
    }
}
