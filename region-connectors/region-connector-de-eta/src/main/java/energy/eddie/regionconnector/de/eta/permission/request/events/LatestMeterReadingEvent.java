package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * Internal event to update the latest meter reading for a permission request.
 * This event does not change the permission status, only updates internal state.
 */
@Entity(name = "DeLatestMeterReadingEvent")
@SuppressWarnings("NullAway")
public class LatestMeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    
    @Column(name = "latest_reading")
    @Nullable
    private final ZonedDateTime latestReading;

    public LatestMeterReadingEvent(String permissionId, ZonedDateTime latestReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestReading = latestReading;
    }

    // No-args constructor for JPA
    protected LatestMeterReadingEvent() {
        super();
        this.latestReading = null;
    }

    @Nullable
    public ZonedDateTime latestReading() {
        return latestReading;
    }
}
