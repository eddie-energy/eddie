package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkInternalPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    private final LocalDate latestMeterReadingEndDate;

    public DkInternalPollingEvent(String permissionId, LocalDate end) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestMeterReadingEndDate = end;
    }

    protected DkInternalPollingEvent() {
        latestMeterReadingEndDate = null;
    }

    public LocalDate latestMeterReadingEndDate() {
        return latestMeterReadingEndDate;
    }
}
