package energy.eddie.regionconnector.us.green.button.permission.events;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "us_green_button", name = "last_meter_readings")
@Entity
@SuppressWarnings("NullAway")
public class MeterReading {
    @SuppressWarnings("unused")
    @Column(name = "permission_id", nullable = false)
    @Id
    private final String permissionId;
    @Id
    @Column(name = "meter_uid", nullable = false)
    private final String meterUid;
    @Column(name = "last_meter_reading")
    @Nullable
    private final ZonedDateTime lastMeterReading;

    public MeterReading(String permissionId, String meterUid, @Nullable ZonedDateTime lastMeterReading) {
        this.permissionId = permissionId;
        this.meterUid = meterUid;
        this.lastMeterReading = lastMeterReading;
    }

    protected MeterReading() {
        permissionId = null;
        meterUid = null;
        lastMeterReading = null;
    }

    public static List<ZonedDateTime> lastMeterReadingDates(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::lastMeterReading).toList();
    }

    @Nullable
    public ZonedDateTime lastMeterReading() {
        return lastMeterReading;
    }

    public static Set<String> allowedMeters(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::meterUid).collect(Collectors.toSet());
    }

    public String meterUid() {
        return meterUid;
    }
}
