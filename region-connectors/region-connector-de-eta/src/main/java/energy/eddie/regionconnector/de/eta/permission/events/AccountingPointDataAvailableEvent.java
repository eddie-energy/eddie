package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaAccountingPointDataAvailableEvent")
public class AccountingPointDataAvailableEvent extends PersistablePermissionEvent {
    public AccountingPointDataAvailableEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.FULFILLED);
    }

    protected AccountingPointDataAvailableEvent() {super();}
}
