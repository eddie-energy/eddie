package energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

@Entity
public class NlSimpleEvent extends NlPermissionEvent {
    public NlSimpleEvent(String permissionId, PermissionProcessStatus status) {
        super(permissionId, status);
    }

    protected NlSimpleEvent() {
        super();
    }
}
