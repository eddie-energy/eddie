package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class RequestPartitions {
    public static final int MAX_AMOUNT_OF_DAYS_BETWEEN_START_AND_END = 184;
    private final FluviusPermissionRequest permissionRequest;
    private final MeterReading meterReading;

    public RequestPartitions(FluviusPermissionRequest permissionRequest, MeterReading meterReading) {
        this.permissionRequest = permissionRequest;
        this.meterReading = meterReading;
    }

    public List<DateTimePartition> partitions() {
        var start = calcEnergyDataStart();
        final var end = calcEnergyDataEnd();
        if (start.isEqual(end)) {
            return List.of(new DateTimePartition(start, end));
        }
        var duration = Duration.between(start, end);
        var numOfPartitions = ((double) duration.toDays()) / MAX_AMOUNT_OF_DAYS_BETWEEN_START_AND_END;
        var list = new ArrayList<DateTimePartition>();
        for (var i = 0; i < numOfPartitions; i++) {
            var next = start.plusDays(MAX_AMOUNT_OF_DAYS_BETWEEN_START_AND_END);
            if (next.isAfter(end)) {
                next = end;
            }
            list.add(new DateTimePartition(start, next));
            start = next;
        }
        return list;
    }

    private ZonedDateTime calcEnergyDataStart() {
        var lastMeterReading = meterReading.lastMeterReading();
        var dataStart = ZonedDateTime.of(permissionRequest.start().atStartOfDay(), ZoneOffset.UTC);
        return Objects.requireNonNullElse(lastMeterReading, dataStart);
    }

    private ZonedDateTime calcEnergyDataEnd() {
        var end = permissionRequest.end().atStartOfDay(ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        return end.isAfter(now) ? now : end;
    }

    public record DateTimePartition(ZonedDateTime start, ZonedDateTime end) {}
}
