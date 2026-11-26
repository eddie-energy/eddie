// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionTest {
    private static final UUID EDDIE_ID = UUID.fromString("e69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private static final UUID PERMISSION_ID = UUID.fromString("f69f9bc2-e16c-4de4-8c3e-00d219dcd819");
    private static final String HANDSHAKE_URL = "https://example.org";
    private static final String ACCESS_TOKEN = "someAccess";

    @Mock
    private OutboundAiidaLocalDataNeed dataNeed;

    private Permission permission;

    @BeforeEach
    void setUp() {
        when(dataNeed.name()).thenReturn("someDataNeed");
        permission = new OutboundPermission(EDDIE_ID,
                                            PERMISSION_ID,
                                            HANDSHAKE_URL,
                                            ACCESS_TOKEN,
                                            UUID.randomUUID(),
                                            dataNeed,
                                            "connection-id",
                                            Instant.parse("2026-01-01T00:00:00Z"),
                                            Instant.parse("2026-12-31T00:00:00Z"),
                                            Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void constructor_setsStatusToAccepted() {
        // When, Then
        assertEquals(PermissionStatus.ACCEPTED, permission.status());
    }

    @Test
    void constructor_setsDataNeedAndDerivesServiceAndDisplayName() {
        // Then
        assertEquals(dataNeed, permission.dataNeed());
        assertEquals("someDataNeed", permission.serviceName());
        assertEquals("someDataNeed", permission.displayName());
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
        var revokeTime = permission.grantTime().minusSeconds(1000);

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> permission.setRevokeTime(revokeTime));
    }

    @Test
    void setDisplayName_overridesDefault() {
        // When
        permission.setDisplayName("My custom name");

        // Then
        assertEquals("My custom name", permission.displayName());
        assertEquals("someDataNeed", permission.serviceName());
    }
}
