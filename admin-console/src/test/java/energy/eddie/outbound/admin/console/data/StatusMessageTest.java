// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.admin.console.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class StatusMessageTest {
    @Test
    void testNoArgConstructor() {
        // When
        StatusMessage statusMessage = new StatusMessage();

        // Then
        assertNull(statusMessage.getId());
        assertNull(statusMessage.getPermissionId());
        assertNull(statusMessage.getRegionConnectorId());
        assertNull(statusMessage.getCountry());
        assertNull(statusMessage.getDso());
        assertNull(statusMessage.getStartDate());
        assertNull(statusMessage.getStatus());
        assertNull(statusMessage.getDataNeedId());
        assertNull(statusMessage.getDescription());
    }
}
