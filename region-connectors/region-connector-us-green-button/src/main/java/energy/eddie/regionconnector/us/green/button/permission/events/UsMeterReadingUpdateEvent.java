package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.process.model.events.InternalPermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Entity
public class UsMeterReadingUpdateEvent extends PersistablePermissionEvent implements InternalPermissionEvent {
    @ElementCollection
    @MapKeyJoinColumn(name = "permission_id", referencedColumnName = "permission_id")
    @CollectionTable(name = "last_meter_readings", joinColumns = @JoinColumn(name = "permission_id"), schema = "us_green_button")
    private final Map<String, ZonedDateTime> lastMeterReadings;
    @Column(name = "polling_status")
    @Enumerated(EnumType.STRING)
    @SuppressWarnings("unused")
    private final PollingStatus pollingStatus;

    public UsMeterReadingUpdateEvent(String permissionId, Map<String, ZonedDateTime> lastMeterReadings) {
        super(permissionId, PermissionProcessStatus.ACCEPTED);
        this.lastMeterReadings = lastMeterReadings;
        this.pollingStatus = PollingStatus.DATA_READY;
    }

    protected UsMeterReadingUpdateEvent() {
        lastMeterReadings = Map.of();
        this.pollingStatus = PollingStatus.DATA_READY;
    }

    public Optional<ZonedDateTime> latestMeterReadingEndDateTime() {
        return DateTimeUtils.oldestDateTime(lastMeterReadings.values());
    }
}
