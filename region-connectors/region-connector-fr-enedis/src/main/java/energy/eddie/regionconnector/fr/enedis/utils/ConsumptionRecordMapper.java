package energy.eddie.regionconnector.fr.enedis.utils;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConsumptionRecordMapper {
    private ConsumptionRecordMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static ConsumptionRecord clcReadingToCIM(ConsumptionLoadCurveMeterReading clcMeterReading) throws IllegalStateException {
        if (clcMeterReading.getIntervalReading().isEmpty()) {
            throw new IllegalStateException("No data available");
        } else {
            ConsumptionRecord clcRecord = new ConsumptionRecord();

            clcRecord.setMeteringPoint(clcMeterReading.getUsagePointId());
            clcRecord.setStartDateTime(DateTimeConverter.isoDateToZonedDateTime(clcMeterReading.getStart()));

            ConsumptionLoadCurveIntervalReading intervalReading = clcMeterReading.getIntervalReading().get(0);
            clcRecord.setMeteringInterval(intervalReading.getIntervalLength().getValue());
            List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
            for (ConsumptionLoadCurveIntervalReading clcInterval : clcMeterReading.getIntervalReading()) {
                ConsumptionPoint consumptionPoint = new ConsumptionPoint();
                consumptionPoint.setConsumption(Double.valueOf(clcInterval.getValue()));
                consumptionPoint.setMeteringType(switch (Objects.requireNonNull(clcInterval.getMeasureType())) {
                    case B -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
                    default -> ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
                });

                consumptionPoints.add(consumptionPoint);
            }
            clcRecord.setConsumptionPoints(consumptionPoints);

            return clcRecord;
        }
    }

    public static ConsumptionRecord dcReadingToCIM(DailyConsumptionMeterReading dcMeterReading) throws IllegalStateException {
        if (dcMeterReading.getIntervalReading().isEmpty()) {
            throw new IllegalStateException("No data available");
        } else {
            ConsumptionRecord dcRecord = new ConsumptionRecord();

            dcRecord.setMeteringPoint(dcMeterReading.getUsagePointId());
            dcRecord.setStartDateTime(DateTimeConverter.isoDateToZonedDateTime(dcMeterReading.getStart()));
            dcRecord.setMeteringInterval(dcMeterReading.getReadingType().getMeasuringPeriod().getValue());

            ConsumptionPoint.MeteringType meteringType = switch (dcMeterReading.getQuality()) {
                case BRUT -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
                default -> ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
            };

            List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
            for (DailyConsumptionIntervalReading dcInterval : dcMeterReading.getIntervalReading()) {
                ConsumptionPoint consumptionPoint = new ConsumptionPoint();
                consumptionPoint.setConsumption(Double.valueOf(dcInterval.getValue()));
                consumptionPoint.setMeteringType(meteringType);

                consumptionPoints.add(consumptionPoint);
            }
            dcRecord.setConsumptionPoints(consumptionPoints);

            return dcRecord;
        }
    }
}