package energy.eddie.aiida.models.permission;

import energy.eddie.api.agnostic.aiida.QrCodeDto;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionTest {
    private final String permissionId = "f69f9bc2-e16c-4de4-8c3e-00d219dcd819";
    private final String serviceName = "My Test Service";
    private final String handshakeUrl = "https://example.org";
    private final String accessToken = "fooBar";
    private final Permission permission = new Permission(new QrCodeDto(permissionId,
                                                                       serviceName,
                                                                       handshakeUrl,
                                                                       accessToken));

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
}
