package energy.eddie.regionconnector.dk.energinet.utils;

import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.regionconnector.dk.energinet.customer.model.*;
import energy.eddie.regionconnector.dk.energinet.enums.PointQualityEnum;
import energy.eddie.regionconnector.dk.energinet.filter.EnerginetResolution;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

public class ConsumptionRecordMapper {
    private ConsumptionRecordMapper() {
        throw new IllegalStateException("Utility class");
    }

    public static ConsumptionRecord timeSeriesToConsumptionRecord(IdentifiableApiResponse response) throws IllegalStateException {
        ConsumptionRecord consumptionRecord = new ConsumptionRecord();
        MyEnergyDataMarketDocumentResponse energyDataMarketDocumentResponse = response.apiResponse();
        consumptionRecord.setMeteringPoint(energyDataMarketDocumentResponse.getId());

        if (energyDataMarketDocumentResponse.getMyEnergyDataMarketDocument() == null) {
            throw new IllegalStateException("No data available");
        }

        MyEnergyDataMarketDocument energyDataMarketDocument = energyDataMarketDocumentResponse.getMyEnergyDataMarketDocument();
        PeriodtimeInterval periodtimeInterval = energyDataMarketDocument.getPeriodTimeInterval();
        consumptionRecord.setStartDateTime(DateTimeConverter.isoDateTimeToZonedDateTime(Objects.requireNonNull(
                periodtimeInterval).getStart(), ZoneOffset.UTC.getId()).withZoneSameInstant(DK_ZONE_ID));

        List<ConsumptionPoint> consumptionPoints = new ArrayList<>();
        for (TimeSeries timeSeries : Objects.requireNonNull(energyDataMarketDocument.getTimeSeries())) {
            for (Period period : Objects.requireNonNull(timeSeries.getPeriod())) {

                consumptionRecord.setMeteringInterval(new EnerginetResolution(period.getResolution()).toISO8601Duration());

                for (Point point : Objects.requireNonNull(period.getPoint())) {
                    var consumptionPoint = new ConsumptionPoint().withMeteringType(switch (PointQualityEnum.fromString(
                            point.getOutQuantityQuality())) {
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
        consumptionRecord.setConnectionId(response.permissionRequest().connectionId());
        consumptionRecord.setPermissionId(response.permissionRequest().permissionId());
        consumptionRecord.setDataNeedId(response.permissionRequest().dataNeedId());
        return consumptionRecord;
    }

    private static String getPointQualityIllegalStateErrorMessage(PointQualityEnum pointQualityEnum) {
        return "Illegal Quality: " + pointQualityEnum;
    }
}
