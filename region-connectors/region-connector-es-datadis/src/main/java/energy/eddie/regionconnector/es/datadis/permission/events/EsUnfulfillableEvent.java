package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsUnfulfillableEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String message;

    public EsUnfulfillableEvent(String permissionId, String message) {
        super(permissionId, PermissionProcessStatus.UNFULFILLABLE);
        this.message = message;
    }

    protected EsUnfulfillableEvent() {
        super();
        message = null;
    }
}
