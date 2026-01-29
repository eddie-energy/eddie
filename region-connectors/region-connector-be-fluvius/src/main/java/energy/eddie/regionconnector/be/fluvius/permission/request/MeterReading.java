// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.be.fluvius.permission.request;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Table(schema = "be_fluvius", name = "last_meter_readings")
@Entity(name = "FluviusMeterReading")
@IdClass(MeterReadingPk.class)
@SuppressWarnings("NullAway")
public class MeterReading {
    @SuppressWarnings("unused")
    @Column(name = "permission_id", nullable = false)
    @Id
    private final String permissionId;
    @Id
    @Column(name = "meter_ean", nullable = false)
    private final String meterEan;
    @Column(name = "last_meter_reading")
    @Nullable
    private ZonedDateTime lastMeterReading;

    public MeterReading(
            String permissionId,
            String meterEan,
            @Nullable ZonedDateTime lastMeterReading
    ) {
        this.permissionId = permissionId;
        this.meterEan = meterEan;
        this.lastMeterReading = lastMeterReading;
    }

    protected MeterReading() {
        permissionId = null;
        meterEan = null;
        lastMeterReading = null;
    }

    public static List<ZonedDateTime> lastMeterReadingDates(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::lastMeterReading).toList();
    }

    public static Set<String> meters(List<MeterReading> readings) {
        return readings.stream().map(MeterReading::meterEan).collect(Collectors.toSet());
    }

    @Nullable
    public ZonedDateTime lastMeterReading() {
        return lastMeterReading;
    }

    public String meterEan() {
        return meterEan;
    }

    public String permissionId() {
        return permissionId;
    }

    public void setLastMeterReading(@Nullable ZonedDateTime lastReading) {
        this.lastMeterReading = lastReading;
    }
}
