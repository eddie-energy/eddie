package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@SuppressWarnings({"NullAway", "unused"})
@Entity(name = "FiMeterReadingEvent")
public class MeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    private final ZonedDateTime latestMeterReading;

    public MeterReadingEvent(String permissionId, ZonedDateTime latestMeterReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestMeterReading = latestMeterReading;
    }

    protected MeterReadingEvent() {
        latestMeterReading = null;
    }

    public ZonedDateTime latestMeterReading() {
        return latestMeterReading;
    }
}
