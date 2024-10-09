package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class MqttCredentialsCreatedEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @Column(name = "mqtt_username", nullable = false)
    @SuppressWarnings("unused")
    private final String mqttUsername;

    @SuppressWarnings("NullAway")
    protected MqttCredentialsCreatedEvent() {
        this.mqttUsername = null;
    }

    public MqttCredentialsCreatedEvent(String permissionId, String mqttUsername) {
        // as this is an internal event, just repeat the previous PermissionProcessStatus
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.mqttUsername = mqttUsername;
    }
}
