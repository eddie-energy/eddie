package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

/**
 * Internal event used to trigger polling for future data.
 * This event is emitted by the CommonFutureDataService to signal the AcceptedHandler
 * to poll data for an active permission request.
 *
 * <p>Implements InternalPermissionEvent to indicate this event should not be
 * propagated to the eligible party.
 */
@Entity(name = "DeStartPollingEvent")
@SuppressWarnings("NullAway")
public class StartPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    public StartPollingEvent(String permissionId) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
    }

    protected StartPollingEvent() {
        super();
    }
}

