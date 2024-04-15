package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "AiidaSimpleEvent")
public class SimpleEvent extends PersistablePermissionEvent {
    public SimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    @SuppressWarnings("NullAway.Init") // Needed for JPA
    protected SimpleEvent() {}
}
