package energy.eddie.regionconnector.es.datadis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;


public final class IntermediateValidatedHistoricalDocument {
    private static final TimeSeriesComplexType.ReasonList REASON_LIST = new TimeSeriesComplexType.ReasonList()
            .withReasons(
                    new ReasonComplexType()
                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
            );
    private final ValidatedHistoricalDataMarketDocument vhd = new ValidatedHistoricalDataMarketDocument()
            .withMRID(UUID.randomUUID().toString())
            .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
            .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
            .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
            .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
            .withProcessProcessType(ProcessTypeList.REALISED);
    private final CommonInformationModelConfiguration cimConfig;
    private final DatadisConfig datadisConfig;
    private final IdentifiableMeteringData identifiableMeteringData;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeteringData identifiableMeteringData,
            CommonInformationModelConfiguration cimConfig,
            DatadisConfig datadisConfig
    ) {
        this.identifiableMeteringData = identifiableMeteringData;
        this.cimConfig = cimConfig;
        this.datadisConfig = datadisConfig;
    }

    public EddieValidatedHistoricalDataMarketDocument eddieValidatedHistoricalDataMarketDocument() {
        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );

        vhd.withSenderMarketParticipantMRID(
                   new PartyIDStringComplexType()
                           .withCodingScheme(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME)
                           .withValue(identifiableMeteringData.permissionRequest()
                                                              .distributorCode()
                                                              .map(DistributorCode::name)
                                                              .orElse("Datadis"))
           )
           .withCreatedDateTime(EsmpDateTime.now().toString())
           .withReceiverMarketParticipantMRID(
                   new PartyIDStringComplexType()
                           .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                           .withValue(datadisConfig.username())
           )
           .withPeriodTimeInterval(
                   new ESMPDateTimeIntervalComplexType()
                           .withStart(timeframe.start())
                           .withEnd(timeframe.end())
           )
           .withTimeSeriesList(timeSeriesList(timeframe));
        var permissionRequest = identifiableMeteringData.permissionRequest();
        return new EddieValidatedHistoricalDataMarketDocument(
                Optional.of(permissionRequest.connectionId()),
                Optional.of(permissionRequest.permissionId()),
                Optional.of(permissionRequest.dataNeedId()),
                vhd
        );
    }

    private ValidatedHistoricalDataMarketDocument.TimeSeriesList timeSeriesList(EsmpTimeInterval timeframe) {
        ValidatedHistoricalDataMarketDocument.TimeSeriesList timeSeriesList = new ValidatedHistoricalDataMarketDocument.TimeSeriesList();
        TimeSeriesComplexType consumptionReading = timeSeriesComplexType(
                timeframe,
                BusinessTypeList.CONSUMPTION,
                DirectionTypeList.DOWN,
                consumptionPoints()
        );
        timeSeriesList.withTimeSeries(consumptionReading);

        if (identifiableMeteringData.permissionRequest().productionSupport()) {
            TimeSeriesComplexType productionReading = timeSeriesComplexType(
                    timeframe,
                    BusinessTypeList.PRODUCTION,
                    DirectionTypeList.UP,
                    productionPoints()
            );
            timeSeriesList.withTimeSeries(productionReading);
        }

        return timeSeriesList;
    }

    private TimeSeriesComplexType timeSeriesComplexType(
            EsmpTimeInterval timeframe,
            BusinessTypeList businessType,
            DirectionTypeList directionTypeList,
            List<PointComplexType> points
    ) {
        return new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withBusinessType(businessType)
                .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                .withFlowDirectionDirection(directionTypeList)
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(AggregateKind.SUM)
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED) // No mapping available
                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME)
                                .withValue(identifiableMeteringData.permissionRequest().meteringPointId())
                )
                .withReasonList(REASON_LIST)
                .withRegisteredResource(
                        new RegisteredResourceComplexType()
                                .withMRID(identifiableMeteringData.permissionRequest().meteringPointId())
                )
                .withSeriesPeriodList(
                        new TimeSeriesComplexType.SeriesPeriodList()
                                .withSeriesPeriods(seriesPeriod(timeframe, points))
                );
    }

    private List<PointComplexType> consumptionPoints() {
        List<PointComplexType> consumptionPoints = new ArrayList<>();
        int position = 0;
        for (MeteringData meteringData
                : identifiableMeteringData.intermediateMeteringData().meteringData()) {
            QualityTypeList qualityTypeList = qualityTypeList(meteringData);
            PointComplexType consumptionPoint = new PointComplexType()
                    .withPosition(String.valueOf(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(meteringData.consumptionKWh()))
                    .withEnergyQuantityQuality(qualityTypeList);
            consumptionPoints.add(consumptionPoint);
            position++;
        }

        return consumptionPoints;
    }

    private List<PointComplexType> productionPoints() {
        List<PointComplexType> productionPoints = new ArrayList<>();
        int position = 0;
        for (MeteringData meteringData
                : identifiableMeteringData.intermediateMeteringData().meteringData()) {
            QualityTypeList qualityTypeList = qualityTypeList(meteringData);
            PointComplexType consumptionPoint = new PointComplexType()
                    .withPosition(String.valueOf(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(meteringData.surplusEnergyKWh()))
                    .withEnergyQuantityQuality(qualityTypeList);
            productionPoints.add(consumptionPoint);
            position++;
        }

        return productionPoints;
    }

    private SeriesPeriodComplexType seriesPeriod(
            EsmpTimeInterval timeInterval,
            List<PointComplexType> points
    ) {
        return new SeriesPeriodComplexType()
                .withResolution(switch (identifiableMeteringData.permissionRequest().measurementType()) {
                    case MeasurementType.QUARTER_HOURLY -> Granularity.PT15M.name();
                    case MeasurementType.HOURLY -> Granularity.PT1H.name();
                })
                .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                          .withStart(timeInterval.start())
                                          .withEnd(timeInterval.end())
                )
                .withPointList(
                        new SeriesPeriodComplexType.PointList()
                                .withPoints(points)
                );
    }

    /**
     * Maps the metering data obtain method to a {@link QualityTypeList}.
     *
     * @param meteringData the metering data
     * @return {@link QualityTypeList#ESTIMATED} if the obtain method is "Estimada", otherwise
     * {@link QualityTypeList#AS_PROVIDED}
     */
    private static QualityTypeList qualityTypeList(MeteringData meteringData) {
        return switch (meteringData.obtainMethod()) {
            case ESTIMATED -> QualityTypeList.ESTIMATED;
            case REAL -> QualityTypeList.AS_PROVIDED;
            case UNKNOWN -> QualityTypeList.NOT_AVAILABLE;
        };
    }
}
