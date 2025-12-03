package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaFulfilledEvent")
public class FulfilledEvent extends PersistablePermissionEvent {

    public FulfilledEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.FULFILLED);
    }

    protected FulfilledEvent() {
        super();
    }
}