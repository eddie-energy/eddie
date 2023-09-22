package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.enums.PointQualityEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConsumptionRecordMapper {
    private static final String ZONE_ID = "Europe/Copenhagen";

    private ConsumptionRecordMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static ConsumptionRecord timeSeriesToCIM(List<MyEnergyDataMarketDocumentResponse> timeSeriesResponse) throws IllegalStateException {
        if (timeSeriesResponse.stream().findFirst().isEmpty()) {
            throw new IllegalStateException("No data available");
        } else {
            ConsumptionRecord consumptionRecord = new ConsumptionRecord();
            MyEnergyDataMarketDocumentResponse energyDataMarketDocumentResponse = timeSeriesResponse.stream().findFirst().get();
            consumptionRecord.setMeteringPoint(energyDataMarketDocumentResponse.getId());

            if (energyDataMarketDocumentResponse.getMyEnergyDataMarketDocument() == null) {
                throw new IllegalStateException("No data available");
            }

            MyEnergyDataMarketDocument energyDataMarketDocument = Objects.requireNonNull(energyDataMarketDocumentResponse).getMyEnergyDataMarketDocument();
            PeriodtimeInterval periodtimeInterval = Objects.requireNonNull(energyDataMarketDocument).getPeriodTimeInterval();
            consumptionRecord.setStartDateTime(DateTimeConverter.isoDateTimeToZonedDateTime(Objects.requireNonNull(periodtimeInterval).getStart(), ZONE_ID));

            List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
            for (TimeSeries timeSeries : Objects.requireNonNull(energyDataMarketDocument.getTimeSeries())) {
                for (Period period : Objects.requireNonNull(timeSeries.getPeriod())) {
                    consumptionRecord.setMeteringInterval(switch (ConsumptionRecord.MeteringInterval.fromValue(Objects.requireNonNull(period.getResolution()))) {
                        case PT_5_M -> ConsumptionRecord.MeteringInterval.PT_5_M;
                        case PT_10_M -> ConsumptionRecord.MeteringInterval.PT_10_M;
                        case PT_15_M -> ConsumptionRecord.MeteringInterval.PT_15_M;
                        case PT_30_M -> ConsumptionRecord.MeteringInterval.PT_30_M;
                        case PT_1_H -> ConsumptionRecord.MeteringInterval.PT_1_H;
                        case P_1_D -> ConsumptionRecord.MeteringInterval.P_1_D;
                        case P_1_M -> ConsumptionRecord.MeteringInterval.P_1_M;
                        case P_1_Y -> ConsumptionRecord.MeteringInterval.P_1_Y;
                    });
                    for (Point point : Objects.requireNonNull(period.getPoint())) {
                        var consumptionPoint = new ConsumptionPoint().withMeteringType(switch (PointQualityEnum.fromString(Objects.requireNonNull(point.getOutQuantityQuality()))) {
                            case A01 ->
                                    throw new IllegalStateException(getPointQualityIllegalStateErrorMessage(PointQualityEnum.A01));
                            case A02 ->
                                    throw new IllegalStateException(getPointQualityIllegalStateErrorMessage(PointQualityEnum.A02));
                            case A03 -> ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
                            case A04 -> ConsumptionPoint.MeteringType.MEASURED_VALUE;
                            case A05 ->
                                    throw new IllegalStateException(getPointQualityIllegalStateErrorMessage(PointQualityEnum.A05));
                        }).withConsumption(Double.valueOf(Objects.requireNonNull(point.getOutQuantityQuantity())));

                        consumptionPoints.add(consumptionPoint);
                    }
                }
            }

            consumptionRecord.setConsumptionPoints(consumptionPoints);
            return consumptionRecord;
        }
    }

    private static String getPointQualityIllegalStateErrorMessage(PointQualityEnum pointQualityEnum) {
        return "Illegal Quality: " + pointQualityEnum;
    }
}

