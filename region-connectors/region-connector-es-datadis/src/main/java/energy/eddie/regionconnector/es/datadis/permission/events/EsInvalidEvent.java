package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsInvalidEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String message;

    public EsInvalidEvent(String permissionId, String message) {
        super(permissionId, PermissionProcessStatus.INVALID);
        this.message = message;
    }

    protected EsInvalidEvent() {
        super();
        message = null;
    }
}
