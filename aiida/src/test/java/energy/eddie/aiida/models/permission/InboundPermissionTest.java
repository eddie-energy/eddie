// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundPermissionTest {
    @Mock
    private InboundAiidaLocalDataNeed dataNeed;

    private InboundPermission permission;

    @BeforeEach
    void setUp() {
        when(dataNeed.name()).thenReturn("someDataNeed");
        permission = new InboundPermission(UUID.randomUUID(),
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
    void constructor_setsInboundMessageFormatToCim1_12() {
        // Then
        assertEquals(InboundMessageFormat.CIM_1_12, permission.inboundMessageFormat());
    }

    @Test
    void updateInboundMessageFormat_updatesField() {
        // When
        permission.updateInboundMessageFormat(InboundMessageFormat.OPENADR_3_1);

        // Then
        assertEquals(InboundMessageFormat.OPENADR_3_1, permission.inboundMessageFormat());
    }

    @Test
    void givenNoDefaults_minAndMaxLimitKw_returnNull() {
        // Then
        assertNull(permission.minLimitKw());
        assertNull(permission.maxLimitKw());
    }

    @Test
    void updateMonitoringDataSource_setsAndClearsDataSource() {
        // Given
        var dataSourceId = UUID.randomUUID();

        // When
        permission.updateMonitoringDataSource(dataSourceId);

        // Then
        assertEquals(dataSourceId, permission.monitoringDataSourceId());

        // When
        permission.updateMonitoringDataSource(null);

        // Then
        assertNull(permission.monitoringDataSourceId());
    }

    @Test
    void givenDataNeedSupportsConnectionLimits_supportsConnectionLimits_returnsTrue() {
        // Given
        when(dataNeed.supportsConnectionLimits()).thenReturn(true);

        // When, Then
        assertTrue(permission.supportsConnectionLimits());
    }

    @Test
    void givenDataNeedWithoutConnectionLimits_supportsConnectionLimits_returnsFalse() {
        // Given
        when(dataNeed.supportsConnectionLimits()).thenReturn(false);

        // When, Then
        assertFalse(permission.supportsConnectionLimits());
    }
}
