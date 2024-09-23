package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Entity
@SuppressWarnings("NullAway")
public class UsMeterReadingUpdateEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @OneToMany(fetch = FetchType.LAZY, targetEntity = MeterReading.class, cascade = CascadeType.MERGE)
    @JoinColumn(insertable = false, updatable = false, name = "permission_id", referencedColumnName = "permission_id")
    private final List<MeterReading> lastMeterReadings;
    @Enumerated(EnumType.STRING)
    @Column(name = "polling_status")
    @SuppressWarnings("unused")
    private final PollingStatus pollingStatus;

    public UsMeterReadingUpdateEvent(
            String permissionId, List<MeterReading> lastMeterReadings, PollingStatus pollingStatus
    ) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
        this.pollingStatus = pollingStatus;
    }

    protected UsMeterReadingUpdateEvent() {
        lastMeterReadings = List.of();
        pollingStatus = null;
    }

    public Optional<ZonedDateTime> latestMeterReadingEndDateTime() {
        return DateTimeUtils.oldestDateTime(MeterReading.lastMeterReadingDates(lastMeterReadings));
    }

    public Set<String> allowedMeters() {
        return MeterReading.allowedMeters(lastMeterReadings);
    }
}
