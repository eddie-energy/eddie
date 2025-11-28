package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaRetransmitRequestedEvent")
public class RetransmitRequestedEvent extends PersistablePermissionEvent {
    public RetransmitRequestedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
    }

    protected RetransmitRequestedEvent() {super();}
}
