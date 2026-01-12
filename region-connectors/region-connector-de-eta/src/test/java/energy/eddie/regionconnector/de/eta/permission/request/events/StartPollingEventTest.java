package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StartPollingEventTest {

    @Test
    void constructor_setsPermissionIdAndStatus() {
        // When
        var event = new StartPollingEvent("test-permission-id");

        // Then
        assertThat(event.permissionId()).isEqualTo("test-permission-id");
        assertThat(event.status()).isEqualTo(PermissionProcessStatus.ACCEPTED);
        assertThat(event.eventCreated()).isNotNull();
    }

    @Test
    void startPollingEvent_implementsInternalPermissionEvent() {
        // When
        var event = new StartPollingEvent("test-permission-id");

        // Then
        assertThat(event).isInstanceOf(InternalPermissionEvent.class);
    }
}

