package energy.eddie.regionconnector.fr.enedis;

import eddie.energy.regionconnector.api.v0.models.ConsumptionPoint;
import eddie.energy.regionconnector.api.v0.models.ConsumptionRecord;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.DailyConsumptionMeterReading;

import java.util.ArrayList;
import java.util.List;

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
            clcRecord.setStartDateTime(DateTimeConverter.ISODateToZonedDateTime(clcMeterReading.getStart()));

            ConsumptionLoadCurveIntervalReading intervalReading = clcMeterReading.getIntervalReading().get(0);
            clcRecord.setMeteringInterval(switch (intervalReading.getIntervalLength()) {
                case PT5M -> ConsumptionRecord.MeteringInterval.PT_5_M;
                case PT10M -> ConsumptionRecord.MeteringInterval.PT_10_M;
                case PT15M -> ConsumptionRecord.MeteringInterval.PT_15_M;
                case PT30M -> ConsumptionRecord.MeteringInterval.PT_30_M;
                case PT60M -> ConsumptionRecord.MeteringInterval.PT_1_H;
                default -> throw new IllegalStateException("Unexpected value: " + intervalReading.getIntervalLength());
            });

            List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
            for (ConsumptionLoadCurveIntervalReading clcInterval : clcMeterReading.getIntervalReading()) {
                ConsumptionPoint consumptionPoint = new ConsumptionPoint();
                consumptionPoint.setConsumption(Double.valueOf(clcInterval.getValue()));
                consumptionPoint.setMeteringType(switch (clcInterval.getMeasureType()) {
                    case B -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
                    default -> ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
                });

                consumptionPoints.add(consumptionPoint);
            }
            clcRecord.setConsumptionPoint(consumptionPoints);

            return clcRecord;
        }
    }

    public static ConsumptionRecord dcReadingToCIM(DailyConsumptionMeterReading dcMeterReading) throws IllegalStateException {
        if (dcMeterReading.getIntervalReading().isEmpty()) {
            throw new IllegalStateException("No data available");
        } else {
            ConsumptionRecord dcRecord = new ConsumptionRecord();

            dcRecord.setMeteringPoint(dcMeterReading.getUsagePointId());
            dcRecord.setStartDateTime(DateTimeConverter.ISODateToZonedDateTime(dcMeterReading.getStart()));
            dcRecord.setMeteringInterval(switch (dcMeterReading.getReadingType().getMeasuringPeriod()) {
                case P1D -> ConsumptionRecord.MeteringInterval.P_1_D;
                default ->
                        throw new IllegalStateException("Unexpected value: " + dcMeterReading.getReadingType().getMeasuringPeriod());
            });

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
            dcRecord.setConsumptionPoint(consumptionPoints);

            return dcRecord;
        }
    }
}

