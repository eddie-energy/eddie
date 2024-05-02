package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class ValidatedEvent extends PersistablePermissionEvent {

    @Transient
    CCMORequest ccmoRequest;

    public ValidatedEvent() {
        super();
        ccmoRequest = null;
    }

    public ValidatedEvent(String permissionId, CCMORequest ccmoRequest) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.ccmoRequest = ccmoRequest;
    }

    public CCMORequest ccmoRequest() {
        return ccmoRequest;
    }
}
