package energy.eddie.regionconnector.fr.enedis.providers.v0;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import energy.eddie.regionconnector.fr.enedis.utils.DateTimeConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class IntermediateConsumptionRecord {
    private final IdentifiableMeterReading meterReading;

    public IntermediateConsumptionRecord(IdentifiableMeterReading meterReading) {
        this.meterReading = meterReading;
    }

    public ConsumptionRecord consumptionRecord() throws IllegalStateException {
        var clcMeterReading = meterReading.payload();
        if (clcMeterReading.getIntervalReading().isEmpty()) {
            throw new IllegalStateException("No data available");
        }
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();

        consumptionRecord.setMeteringPoint(clcMeterReading.getUsagePointId());
        consumptionRecord.setStartDateTime(DateTimeConverter.isoDateToZonedDateTime(clcMeterReading.getStart()));
        ConsumptionLoadCurveIntervalReading intervalReading = clcMeterReading.getIntervalReading().getFirst();
        consumptionRecord.setMeteringInterval(intervalReading.getIntervalLength().getValue());


        List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
        for (ConsumptionLoadCurveIntervalReading clcInterval : clcMeterReading.getIntervalReading()) {
            ConsumptionPoint consumptionPoint = new ConsumptionPoint();
            consumptionPoint.setConsumption(Double.valueOf(clcInterval.getValue()));
            consumptionPoint.setMeteringType(
                    Objects.requireNonNull(clcInterval.getMeasureType()) == ConsumptionLoadCurveIntervalReading.MeasureTypeEnum.B
                            ? ConsumptionPoint.MeteringType.MEASURED_VALUE
                            : ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE
            );

            consumptionPoints.add(consumptionPoint);
        }

        consumptionRecord.setConsumptionPoints(consumptionPoints);
        consumptionRecord.setPermissionId(meterReading.permissionId());
        consumptionRecord.setConnectionId(meterReading.connectionId());
        consumptionRecord.setDataNeedId(meterReading.dataNeedId());
        return consumptionRecord;

    }
}

