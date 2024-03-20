package energy.eddie.regionconnector.es.datadis;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

public class ConsumptionRecordMapper {
    public static final int CONVERSION_FACTOR = 1000;

    private ConsumptionRecordMapper() {
    }

    public static ConsumptionRecord mapToMvp1ConsumptionRecord(IntermediateMeteringData meteringData, @Nullable String permissionId, @Nullable String connectionId, MeasurementType measurementType, @Nullable String dataNeedId) {
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        consumptionRecord.setPermissionId(permissionId);
        consumptionRecord.setConnectionId(connectionId);
        consumptionRecord.setDataNeedId(dataNeedId);


        var firstMeteringData = meteringData.meteringData().getFirst();

        consumptionRecord.setMeteringPoint(firstMeteringData.cups());
        consumptionRecord.setStartDateTime(meteringData.start().atStartOfDay(ZONE_ID_SPAIN));

        var consumptionPoints = meteringData.meteringData().stream().map(reading -> {
            ConsumptionPoint consumptionPoint = new ConsumptionPoint();

            var consumptionWh = reading.consumptionKWh() * CONVERSION_FACTOR;
            consumptionPoint.setConsumption(consumptionWh);
            consumptionPoint.setMeteringType(meteringType(reading));

            return consumptionPoint;
        }).toList();
        consumptionRecord.setConsumptionPoints(consumptionPoints);

        consumptionRecord.setMeteringInterval(measurementType == MeasurementType.HOURLY ? Granularity.PT1H.name() : Granularity.PT15M.name());
        return consumptionRecord;
    }

    @NotNull
    private static ConsumptionPoint.MeteringType meteringType(MeteringData reading) {
        return switch (reading.obtainMethod()) {
            case REAL -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
            case ESTIMATED, UNKNOWN ->
                    ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE; // we don't have a specific value for UNKNOWN
        };
    }
}
