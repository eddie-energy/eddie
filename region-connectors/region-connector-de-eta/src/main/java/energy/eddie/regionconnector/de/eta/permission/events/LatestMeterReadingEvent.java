package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "LatestMeterReadingEvent")
public class LatestMeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    @Column(name = "latest_reading")
    private final ZonedDateTime latestReading;

    public LatestMeterReadingEvent(String permissionId, ZonedDateTime latestReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestReading = latestReading;
    }

    protected LatestMeterReadingEvent() {
        super();
        this.latestReading = null;
    }

    public ZonedDateTime latestReading() {
        return latestReading;
    }
}