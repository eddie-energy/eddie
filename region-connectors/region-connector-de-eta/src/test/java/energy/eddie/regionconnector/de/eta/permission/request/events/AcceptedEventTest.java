package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptedEventTest {

    @Test
    void constructor_setsPermissionIdStatusAndTokens() {
        var event = new AcceptedEvent("test-permission-id", "access-token", "refresh-token");

        assertThat(event.permissionId()).isEqualTo("test-permission-id");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.ACCEPTED);
        assertThat(event.accessToken()).isEqualTo("access-token");
        assertThat(event.refreshToken()).contains("refresh-token");
        assertThat(event.eventCreated()).isNotNull();
    }

    @Test
    void constructor_allowsNullRefreshToken() {
        var event = new AcceptedEvent("test-permission-id", "access-token", null);

        assertThat(event.accessToken()).isEqualTo("access-token");
        assertThat(event.refreshToken()).isEmpty();
    }
}

