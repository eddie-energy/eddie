package energy.eddie.regionconnector.us.green.button.permission.events;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "us_green_button", name = "last_meter_readings")
@Entity
@IdClass(MeterReadingPk.class)
@SuppressWarnings("NullAway")
public class MeterReading implements Persistable<MeterReadingPk> {
    @SuppressWarnings("unused")
    @Column(name = "permission_id", nullable = false)
    @Id
    private final String permissionId;
    @Id
    @Column(name = "meter_uid", nullable = false)
    private final String meterUid;
    @Column(name = "last_meter_reading")
    @Nullable
    private ZonedDateTime lastMeterReading;

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

    @Override
    public MeterReadingPk getId() {
        return null;
    }

    @Override
    public boolean isNew() {
        return lastMeterReading == null;
    }

    public void setLastMeterReading(@Nullable ZonedDateTime lastReading) {
        this.lastMeterReading = lastReading;
    }
}
