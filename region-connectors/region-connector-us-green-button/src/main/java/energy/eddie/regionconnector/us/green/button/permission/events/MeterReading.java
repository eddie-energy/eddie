package energy.eddie.regionconnector.us.green.button.permission.events;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "us_green_button", name = "last_meter_readings")
@Entity
@IdClass(MeterReadingPk.class)
@SuppressWarnings("NullAway")
public class MeterReading {
    @SuppressWarnings("unused")
    @Column(name = "permission_id", nullable = false)
    @Id
    private final String permissionId;
    @Id
    @Column(name = "meter_uid", nullable = false)
    private final String meterUid;
    @Enumerated(EnumType.STRING)
    @Column(name = "historical_collection_status", columnDefinition = "text")
    private final PollingStatus historicalCollectionStatus;
    @Column(name = "last_meter_reading")
    @Nullable
    private ZonedDateTime lastMeterReading;

    public MeterReading(
            String permissionId,
            String meterUid,
            @Nullable ZonedDateTime lastMeterReading,
            PollingStatus historicalCollectionStatus
    ) {
        this.permissionId = permissionId;
        this.meterUid = meterUid;
        this.lastMeterReading = lastMeterReading;
        this.historicalCollectionStatus = historicalCollectionStatus;
    }

    protected MeterReading() {
        permissionId = null;
        meterUid = null;
        lastMeterReading = null;
        historicalCollectionStatus = null;
    }

    public static List<ZonedDateTime> lastMeterReadingDates(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::lastMeterReading).toList();
    }

    public static Set<String> allowedMeters(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::meterUid).collect(Collectors.toSet());
    }

    @Nullable
    public ZonedDateTime lastMeterReading() {
        return lastMeterReading;
    }

    public String meterUid() {
        return meterUid;
    }

    public String permissionId() {
        return permissionId;
    }

    public void setLastMeterReading(@Nullable ZonedDateTime lastReading) {
        this.lastMeterReading = lastReading;
    }

    public boolean isReadyToPoll() {
        return historicalCollectionStatus == PollingStatus.DATA_READY;
    }
}
