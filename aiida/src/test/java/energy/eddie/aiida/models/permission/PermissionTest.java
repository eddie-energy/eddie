// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionTest {
    private final UUID eddieId = UUID.fromString("e69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final UUID permissionId = UUID.fromString("f69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private final String handshakeUrl = "https://example.org";
    private final String accessToken = "someAccess";
    private final Permission permission = new Permission(eddieId,
                                                         permissionId,
                                                         handshakeUrl,
                                                         accessToken,
                                                         UUID.randomUUID());

    @Test
    void constructor_setsStatusToCreated() {
        // When, Then
        assertEquals(PermissionStatus.CREATED, permission.status());
    }

    @Test
    void givenNull_setStatus_throws() {
        // When, Then
        assertThrows(NullPointerException.class, () -> permission.setStatus(null));
    }

    @Test
    void givenNull_setRevokeTime_throws() {
        // When, Then
        assertThrows(NullPointerException.class, () -> permission.setRevokeTime(null));
    }

    @Test
    void givenRevocationTimeBeforeGrantTime_setRevokeTime_throws() {
        // Given
        var start = Instant.now();
        permission.setGrantTime(start);
        var revokeTime = start.minusSeconds(1000);

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> permission.setRevokeTime(revokeTime));
    }

    @Test
    void setDataNeed_setsDataNeedAndServiceName() {
        // Given
        var dataNeed = mock(AiidaLocalDataNeed.class);
        when(dataNeed.name()).thenReturn("someDataNeed");

        // When
        permission.setDataNeed(dataNeed);

        // Then
        assertEquals(dataNeed, permission.dataNeed());
        assertEquals("someDataNeed", permission.serviceName());
    }

    @Test
    void givenNull_setDataNeed_throws() {
        // When, Then
        assertThrows(NullPointerException.class, () -> permission.setDataNeed(null));
    }
}
