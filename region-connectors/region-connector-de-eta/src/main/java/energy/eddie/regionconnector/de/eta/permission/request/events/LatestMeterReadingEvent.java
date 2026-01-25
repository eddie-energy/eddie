package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.de.eta.permission.request.events.PersistablePermissionEvent;
import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.springframework.lang.Nullable;

import java.time.ZonedDateTime;

@Entity(name = "DeEtaLatestMeterReadingEvent")
@DiscriminatorValue("LATEST_READING")
public class LatestMeterReadingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {

    @Column(name = "latest_reading")
    @Nullable
    private final ZonedDateTime latestReading;

    protected LatestMeterReadingEvent() {
        super();
        this.latestReading = null;
    }

    public LatestMeterReadingEvent(String permissionId, ZonedDateTime latestReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestReading = latestReading;
    }

    @Nullable
    public ZonedDateTime latestReading() {
        return latestReading;
    }
}