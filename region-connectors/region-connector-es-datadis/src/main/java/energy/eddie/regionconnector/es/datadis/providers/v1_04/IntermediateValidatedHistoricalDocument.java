package energy.eddie.regionconnector.es.datadis.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.config.DatadisConfig;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.ToDoubleFunction;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

public final class IntermediateValidatedHistoricalDocument {
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

    public VHDEnvelope value() {
        var timeframe = new EsmpTimeInterval(
                identifiableMeteringData.intermediateMeteringData().start(),
                identifiableMeteringData.intermediateMeteringData().end(),
                ZONE_ID_SPAIN
        );

        var receiverCodingScheme = StandardCodingSchemeTypeList.fromValue(
                cimConfig.eligiblePartyNationalCodingScheme().value()
        );
        var vhd = new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                .withValue(identifiableMeteringData.permissionRequest()
                                                                   .distributorCode()
                                                                   .map(DistributorCode::name)
                                                                   .orElse("Datadis"))
                )
                .withCreatedDateTime(ZonedDateTime.now(ZONE_ID_SPAIN))
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(receiverCodingScheme.value())
                                .withValue(datadisConfig.username())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeries(timeSeriesList(timeframe));
        return new VhdEnvelopeWrapper(vhd, identifiableMeteringData.permissionRequest()).wrap();
    }

    private List<TimeSeries> timeSeriesList(EsmpTimeInterval timeframe) {
        var list = new ArrayList<>(timeSeries(
                timeframe,
                StandardBusinessTypeList.CONSUMPTION,
                StandardDirectionTypeList.DOWN,
                consumptionPoints()
        ));

        if (identifiableMeteringData.permissionRequest().productionSupport()) {
            list.addAll(timeSeries(
                    timeframe,
                    StandardBusinessTypeList.PRODUCTION,
                    StandardDirectionTypeList.UP,
                    productionPoints()
            ));
        }
        return list;
    }

    private List<TimeSeries> timeSeries(
            EsmpTimeInterval timeframe,
            StandardBusinessTypeList businessType,
            StandardDirectionTypeList directionTypeList,
            Map<String, List<Point>> points
    ) {
        var list = new ArrayList<TimeSeries>();
        for (var cups : points.entrySet()) {
            list.add(
                    new TimeSeries()
                            .withVersion("1")
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(businessType.value())
                            .withProduct(StandardEnergyProductTypeList.ACTIVE_ENERGY.value())
                            .withFlowDirectionDirection(directionTypeList.value())
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregate(AggregateKind.SUM)
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED) // No mapping available
                            .withEnergyMeasurementUnitName(StandardUnitOfMeasureTypeList.KILOWATT_HOUR.value())
                            .withMarketEvaluationPointMRID(
                                    new MeasurementPointIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(identifiableMeteringData.permissionRequest().meteringPointId())
                            )
                            .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                            .withRegisteredResourceMRID(
                                    new ResourceIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.SPAIN_NATIONAL_CODING_SCHEME.value())
                                            .withValue(cups.getKey())
                            )
                            .withPeriods(seriesPeriod(timeframe, cups.getValue()))
            );
        }
        return list;
    }

    private Map<String, List<Point>> consumptionPoints() {
        return toPoints(MeteringData::consumptionKWh);
    }

    private Map<String, List<Point>> productionPoints() {
        return toPoints(MeteringData::surplusEnergyKWh);
    }

    private Map<String, List<Point>> toPoints(ToDoubleFunction<MeteringData> energyAccessor) {
        Map<String, List<Point>> points = new HashMap<>();
        int position = 1;
        for (MeteringData meteringData : identifiableMeteringData.intermediateMeteringData().meteringData()) {
            StandardQualityTypeList qualityTypeList = qualityTypeList(meteringData);
            Point consumptionPoint = new Point()
                    .withPosition(position)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(energyAccessor.applyAsDouble(meteringData)))
                    .withEnergyQuantityQuality(qualityTypeList.value());
            points.putIfAbsent(meteringData.cups(), new ArrayList<>());
            points.get(meteringData.cups()).add(consumptionPoint);
            position++;
        }
        return points;
    }

    private SeriesPeriod seriesPeriod(
            EsmpTimeInterval timeInterval,
            List<Point> points
    ) {
        var dataTypeFactory = DatatypeFactory.newDefaultInstance();
        var granularity = switch (identifiableMeteringData.permissionRequest().measurementType()) {
            case MeasurementType.QUARTER_HOURLY -> Granularity.PT15M;
            case MeasurementType.HOURLY -> Granularity.PT1H;
        };
        return new SeriesPeriod()
                .withResolution(dataTypeFactory.newDuration(granularity.duration().toMillis()))
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(timeInterval.start())
                                .withEnd(timeInterval.end())
                )
                .withPoints(points);
    }

    /**
     * Maps the metering data obtain method to a {@link StandardQualityTypeList}.
     *
     * @param meteringData the metering data
     * @return {@link StandardQualityTypeList#ESTIMATED} if the obtain method is "Estimada", otherwise
     * {@link StandardQualityTypeList#AS_PROVIDED}
     */
    private static StandardQualityTypeList qualityTypeList(MeteringData meteringData) {
        return switch (meteringData.obtainMethod()) {
            case ESTIMATED -> StandardQualityTypeList.ESTIMATED;
            case REAL -> StandardQualityTypeList.AS_PROVIDED;
            case UNKNOWN -> StandardQualityTypeList.NOT_AVAILABLE;
        };
    }
}
