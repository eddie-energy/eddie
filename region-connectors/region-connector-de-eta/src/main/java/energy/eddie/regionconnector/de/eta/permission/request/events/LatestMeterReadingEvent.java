package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDate;

/**
 * Internal event used to track the latest meter reading for a permission request.
 * This event is used to update the state of a permission request without changing
 * the PermissionProcessStatus.
 * 
 * <p>Since this implements {@link InternalPermissionEvent}, it will be persisted
 * but not propagated to the eligible party as an integration event.</p>
 */
@Entity(name = "DeLatestMeterReadingEvent")
@SuppressWarnings({"NullAway", "unused"})
public class LatestMeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    @Column(name = "latest_meter_reading")
    private final LocalDate latestMeterReading;

    public LatestMeterReadingEvent(String permissionId, LocalDate latestMeterReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestMeterReading = latestMeterReading;
    }

    protected LatestMeterReadingEvent() {
        super();
        this.latestMeterReading = null;
    }

    public LocalDate latestMeterReading() {
        return latestMeterReading;
    }
}

