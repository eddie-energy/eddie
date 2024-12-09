package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@SuppressWarnings("NullAway")
public class UsMeterReadingUpdateEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @OneToMany(fetch = FetchType.LAZY, targetEntity = MeterReading.class)
    @JoinColumn(insertable = false, updatable = false, name = "permission_id", referencedColumnName = "permission_id")
    private final List<MeterReading> lastMeterReadings;

    public UsMeterReadingUpdateEvent(
            String permissionId, List<MeterReading> lastMeterReadings
    ) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
    }

    protected UsMeterReadingUpdateEvent() {
        lastMeterReadings = List.of();
    }

    public Optional<ZonedDateTime> latestMeterReadingEndDateTime() {
        return DateTimeUtils.oldestDateTime(MeterReading.lastMeterReadingDates(List.copyOf(lastMeterReadings)));
    }
}
