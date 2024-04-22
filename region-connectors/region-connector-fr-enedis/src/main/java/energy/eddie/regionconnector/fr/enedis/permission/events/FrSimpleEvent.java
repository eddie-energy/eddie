package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrSimpleEvent extends PersistablePermissionEvent {
    public FrSimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected FrSimpleEvent() {
        super();
    }
}
