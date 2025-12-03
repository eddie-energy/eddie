package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaValidatedHistoricalDataAvailableEvent")
public class ValidatedHistoricalDataAvailableEvent extends PersistablePermissionEvent {
    public ValidatedHistoricalDataAvailableEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.FULFILLED);
    }

    protected ValidatedHistoricalDataAvailableEvent() {super();}
}
