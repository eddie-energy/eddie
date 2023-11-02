package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.enums.PeriodResolutionEnum;
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
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        MyEnergyDataMarketDocumentResponse energyDataMarketDocumentResponse = timeSeriesResponse.stream().findFirst().orElseThrow();
        consumptionRecord.setMeteringPoint(energyDataMarketDocumentResponse.getId());

        if (energyDataMarketDocumentResponse.getMyEnergyDataMarketDocument() == null) {
            throw new IllegalStateException("No data available");
        }

        MyEnergyDataMarketDocument energyDataMarketDocument = energyDataMarketDocumentResponse.getMyEnergyDataMarketDocument();
        PeriodtimeInterval periodtimeInterval = energyDataMarketDocument.getPeriodTimeInterval();
        consumptionRecord.setStartDateTime(DateTimeConverter.isoDateTimeToZonedDateTime(Objects.requireNonNull(periodtimeInterval).getStart(), ZONE_ID));

        List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
        for (TimeSeries timeSeries : Objects.requireNonNull(energyDataMarketDocument.getTimeSeries())) {
            for (Period period : Objects.requireNonNull(timeSeries.getPeriod())) {
                consumptionRecord.setMeteringInterval(switch (PeriodResolutionEnum.fromString(period.getResolution())) {
                    case PT15M -> ConsumptionRecord.MeteringInterval.PT_15_M;
                    case PT1H -> ConsumptionRecord.MeteringInterval.PT_1_H;
                    case PT1D -> ConsumptionRecord.MeteringInterval.P_1_D;
                    case P1M -> ConsumptionRecord.MeteringInterval.P_1_M;
                    case P1Y -> ConsumptionRecord.MeteringInterval.P_1_Y;
                });
                for (Point point : Objects.requireNonNull(period.getPoint())) {
                    var consumptionPoint = new ConsumptionPoint().withMeteringType(switch (PointQualityEnum.fromString(point.getOutQuantityQuality())) {
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

    private static String getPointQualityIllegalStateErrorMessage(PointQualityEnum pointQualityEnum) {
        return "Illegal Quality: " + pointQualityEnum;
    }
}

