package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity(name = "DeEtaAcceptedEvent")
@DiscriminatorValue("ACCEPTED")
public class AcceptedEvent extends PersistablePermissionEvent {

    protected AcceptedEvent() {
        super();
    }

    public AcceptedEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }
}