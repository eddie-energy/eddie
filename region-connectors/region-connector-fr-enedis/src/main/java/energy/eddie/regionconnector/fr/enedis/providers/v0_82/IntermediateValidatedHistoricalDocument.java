package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiVersion;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveIntervalReading;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class IntermediateValidatedHistoricalDocument {
    public static final DateTimeFormatter ENEDIS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final ZoneId ECT = ZoneId.of("Europe/Paris");
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
            .withProcessProcessType(ProcessTypeList.REALISED)
            .withSenderMarketParticipantMRID(
                    new PartyIDStringComplexType()
                            .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                            .withValue("ENEDIS") // No Mapping
            );
    private final CommonInformationModelConfiguration cimConfig;
    private final EnedisConfiguration enedisConfig;
    private final IdentifiableMeterReading meterReading;

    IntermediateValidatedHistoricalDocument(IdentifiableMeterReading identifiableMeterReading,
                                            CommonInformationModelConfiguration cimConfig,
                                            EnedisConfiguration enedisConfig) {
        this.meterReading = identifiableMeterReading;
        this.cimConfig = cimConfig;
        this.enedisConfig = enedisConfig;
    }

    public EddieValidatedHistoricalDataMarketDocument eddieValidatedHistoricalDataMarketDocument() {
        var timeframe = new EsmpTimeInterval(
                consumptionLoadCurveMeterReading().getStart(),
                consumptionLoadCurveMeterReading().getEnd(),
                ENEDIS_DATE_FORMAT,
                ECT
        );
        vhd
                .withCreatedDateTime(EsmpDateTime.now().toString())
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                .withValue(enedisConfig.clientId())
                )
                .withPeriodTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(timeframe.start())
                                .withEnd(timeframe.end())
                )
                .withTimeSeriesList(timeSeriesList());
        return new EddieValidatedHistoricalDataMarketDocument(
                Optional.of(meterReading.connectionId()),
                Optional.of(meterReading.permissionId()),
                Optional.of(meterReading.dataNeedId()),
                vhd
        );
    }

    private ValidatedHistoricalDataMarketDocument.TimeSeriesList timeSeriesList() {
        TimeSeriesComplexType reading = new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withBusinessType(BusinessTypeList.CONSUMPTION)
                .withProduct(energyProductTypeList())
                .withVersion(EnedisApiVersion.V5.name())
                .withFlowDirectionDirection(DirectionTypeList.DOWN)
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(aggregateKind())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.NONE) // No mapping available
                .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.WATT)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                .withValue(consumptionLoadCurveMeterReading().getUsagePointId())
                )
                .withReasonList(REASON_LIST)
                .withSeriesPeriodList(
                        new TimeSeriesComplexType.SeriesPeriodList()
                                .withSeriesPeriods(seriesPeriods())
                );
        return new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                .withTimeSeries(List.of(reading));
    }

    @Nullable
    private EnergyProductTypeList energyProductTypeList() {
        if (consumptionLoadCurveMeterReading().getReadingType() == null) {
            return null;
        }
        String measurementKind = consumptionLoadCurveMeterReading().getReadingType().getMeasurementKind();
        if (measurementKind == null) {
            return null;
        }
        if (measurementKind.equals("power")) {
            return EnergyProductTypeList.ACTIVE_POWER;
        }
        throw new IllegalStateException("Unknown measurement kind '%s'".formatted(measurementKind));
    }

    @Nullable
    private AggregateKind aggregateKind() {
        if (consumptionLoadCurveMeterReading().getReadingType() == null ||
                consumptionLoadCurveMeterReading().getReadingType().getAggregate() == null) {
            return null;
        }
        String aggregate = consumptionLoadCurveMeterReading().getReadingType().getAggregate();
        return AggregateKind.valueOf(aggregate.toUpperCase(Locale.ROOT));
    }

    private List<SeriesPeriodComplexType> seriesPeriods() {
        var interval = new EsmpTimeInterval(
                consumptionLoadCurveMeterReading().getStart(),
                consumptionLoadCurveMeterReading().getEnd(),
                ENEDIS_DATE_FORMAT,
                ECT
        );
        var consumptionLoadCurveMeterReading = consumptionLoadCurveMeterReading();
        List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
        int position = 0;
        for (ConsumptionLoadCurveIntervalReading intervalReading
                : consumptionLoadCurveMeterReading.getIntervalReading()) {
            var seriesPeriod = new SeriesPeriodComplexType()
                    .withResolution(intervalReading.getIntervalLength().getValue())
                    .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                            .withStart(interval.start())
                            .withEnd(interval.end())
                    )
                    .withPointList(
                            new SeriesPeriodComplexType.PointList()
                                    .withPoints(List.of(
                                            new PointComplexType()
                                                    .withPosition("%d".formatted(position))
                                                    .withEnergyQuantityQuantity(new BigDecimal(intervalReading.getValue()))
                                                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED)
                                    ))
                    );
            seriesPeriods.add(seriesPeriod);
            position++;
        }
        return seriesPeriods;
    }

    private ConsumptionLoadCurveMeterReading consumptionLoadCurveMeterReading() {
        return this.meterReading.payload();
    }
}
