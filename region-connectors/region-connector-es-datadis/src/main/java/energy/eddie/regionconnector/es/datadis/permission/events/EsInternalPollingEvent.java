package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsInternalPollingEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    private final LocalDate latestMeterReading;

    public EsInternalPollingEvent(String permissionId, LocalDate latestMeterReading) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.latestMeterReading = latestMeterReading;
    }

    protected EsInternalPollingEvent() {
        latestMeterReading = null;
    }
}
