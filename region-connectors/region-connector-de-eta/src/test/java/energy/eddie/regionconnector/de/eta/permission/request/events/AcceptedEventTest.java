package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AcceptedEventTest {

    @Test
    void constructor_setsPermissionIdAndStatus() {
        // When
        var event = new AcceptedEvent("test-permission-id");

        // Then
        assertThat(event.permissionId()).isEqualTo("test-permission-id");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.ACCEPTED);
        assertThat(event.eventCreated()).isNotNull();
    }
}

