// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundPermissionTest {
    private static final CronExpression DATA_NEED_SCHEDULE = CronExpression.parse("*/5 * * * * *");

    @Mock
    private OutboundAiidaLocalDataNeed dataNeed;

    private OutboundPermission permission;

    @BeforeEach
    void setUp() {
        when(dataNeed.name()).thenReturn("someDataNeed");
        permission = new OutboundPermission(UUID.randomUUID(),
                                            UUID.randomUUID(),
                                            "https://example.org",
                                            "someAccess",
                                            UUID.randomUUID(),
                                            dataNeed,
                                            "connection-id",
                                            Instant.parse("2026-01-01T00:00:00Z"),
                                            Instant.parse("2026-12-31T00:00:00Z"),
                                            Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void constructor_enablesTransmission() {
        // Then
        assertTrue(permission.transmissionEnabled());
    }

    @Test
    void setTransmissionEnabled_updatesField() {
        // When
        permission.setTransmissionEnabled(false);

        // Then
        assertFalse(permission.transmissionEnabled());
    }

    @Test
    void givenNoOwnSchedule_effectiveTransmissionSchedule_returnsScheduleOfDataNeed() {
        // Given
        when(dataNeed.transmissionSchedule()).thenReturn(DATA_NEED_SCHEDULE);

        // When, Then
        assertEquals(DATA_NEED_SCHEDULE, permission.effectiveTransmissionSchedule());
    }

    @Test
    void givenOwnSchedule_effectiveTransmissionSchedule_returnsOwnSchedule() {
        // Given
        var ownSchedule = CronExpression.parse("0 * * * * *");
        permission.setTransmissionSchedule(ownSchedule);

        // When, Then
        assertEquals(ownSchedule, permission.effectiveTransmissionSchedule());
    }

    @Test
    void givenNull_setTransmissionSchedule_throws() {
        // When, Then
        assertThrows(NullPointerException.class, () -> permission.setTransmissionSchedule(null));
    }
}
