package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

public class ConsumptionRecordMapper {
    public static final int CONVERSION_FACTOR = 1000;

    private ConsumptionRecordMapper() {
    }

    public static ConsumptionRecord mapToMvp1ConsumptionRecord(List<MeteringData> meteringData, @Nullable String permissionId, @Nullable String connectionId, MeasurementType measurementType, @Nullable String dataNeedId) throws InvalidMappingException {
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        consumptionRecord.setPermissionId(permissionId);
        consumptionRecord.setConnectionId(connectionId);
        consumptionRecord.setDataNeedId(dataNeedId);

        if (meteringData.isEmpty()) {
            throw new InvalidMappingException("No metering data found");
        }

        consumptionRecord.setMeteringPoint(meteringData.getFirst().cups());

        LocalDateTime startDateTime = readingToDateTime(meteringData.getFirst());
        consumptionRecord.setStartDateTime(startDateTime.atZone(ZONE_ID_SPAIN));

        var consumptionPoints = meteringData.stream().map(reading -> {
            ConsumptionPoint consumptionPoint = new ConsumptionPoint();

            var consumptionWh = reading.consumptionKWh() * CONVERSION_FACTOR;
            consumptionPoint.setConsumption(consumptionWh);
            if (reading.obtainMethod() != null && reading.obtainMethod().contentEquals("Real")) {
                consumptionPoint.setMeteringType(ConsumptionPoint.MeteringType.MEASURED_VALUE);
            } else {
                consumptionPoint.setMeteringType(ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE);
            }

            return consumptionPoint;
        }).toList();
        consumptionRecord.setConsumptionPoints(consumptionPoints);

        consumptionRecord.setMeteringInterval(measurementType == MeasurementType.HOURLY ? Granularity.PT1H.name() : Granularity.PT15M.name());
        return consumptionRecord;
    }

    private static LocalDateTime readingToDateTime(MeteringData reading) throws InvalidMappingException {
        LocalDate date = reading.date();
        LocalTime time = reading.time();

        if (date == null || time == null) {
            throw new InvalidMappingException("No date or time found");
        }

        return LocalDateTime.of(date, time);
    }
}