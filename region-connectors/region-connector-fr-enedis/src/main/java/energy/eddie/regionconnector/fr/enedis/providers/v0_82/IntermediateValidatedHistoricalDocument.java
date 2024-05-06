package energy.eddie.regionconnector.fr.enedis.providers.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.v0_82.cim.EddieValidatedHistoricalDataMarketDocument;
import energy.eddie.api.v0_82.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.client.EnedisApiVersion;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.IntervalReading;
import energy.eddie.regionconnector.fr.enedis.dto.MeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.utils.EsmpDateTime;
import energy.eddie.regionconnector.shared.utils.EsmpTimeInterval;

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
    private final IdentifiableMeterReading identifiableMeterReading;

    IntermediateValidatedHistoricalDocument(
            IdentifiableMeterReading identifiableMeterReading,
            CommonInformationModelConfiguration cimConfig,
            EnedisConfiguration enedisConfig
    ) {
        this.identifiableMeterReading = identifiableMeterReading;
        this.cimConfig = cimConfig;
        this.enedisConfig = enedisConfig;
    }

    public EddieValidatedHistoricalDataMarketDocument eddieValidatedHistoricalDataMarketDocument() {
        var timeframe = new EsmpTimeInterval(
                meterReading().start().format(ENEDIS_DATE_FORMAT),
                meterReading().end().format(ENEDIS_DATE_FORMAT),
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
        FrEnedisPermissionRequest permissionRequest = identifiableMeterReading.permissionRequest();
        return new EddieValidatedHistoricalDataMarketDocument(
                Optional.of(permissionRequest.connectionId()),
                Optional.of(permissionRequest.permissionId()),
                Optional.of(permissionRequest.dataNeedId()),
                vhd
        );
    }

    private MeterReading meterReading() {
        return this.identifiableMeterReading.meterReading();
    }

    private ValidatedHistoricalDataMarketDocument.TimeSeriesList timeSeriesList() {
        TimeSeriesComplexType reading = new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withBusinessType(BusinessTypeList.CONSUMPTION)
                .withProduct(energyProductTypeList())
                .withVersion(EnedisApiVersion.V5.name())
                .withFlowDirectionDirection(DirectionTypeList.DOWN)
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeAggregation(aggregateKind())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.FRANCE_NATIONAL_CODING_SCHEME)
                                .withValue(meterReading().usagePointId())
                )
                .withReasonList(REASON_LIST);

        reading = switch (meterReading().readingType().unit()) {
            case "W", "VA" -> reading.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.WATT)
                                     .withSeriesPeriodList(seriesPeriods(false));
            case "Wh" -> reading.withEnergyMeasurementUnitName(UnitOfMeasureTypeList.KILOWATT_HOUR)
                                .withSeriesPeriodList(seriesPeriods(true));
            default -> reading.withSeriesPeriodList(seriesPeriods(false));
        };

        return new ValidatedHistoricalDataMarketDocument.TimeSeriesList()
                .withTimeSeries(List.of(reading));
    }

    private EnergyProductTypeList energyProductTypeList() {
        return switch (meterReading().readingType().measurementKind()) {
            case "power" -> EnergyProductTypeList.ACTIVE_POWER;
            case "energy" -> EnergyProductTypeList.ACTIVE_ENERGY;
            default -> throw new IllegalStateException("Unknown measurement kind '%s'".formatted(
                    meterReading().readingType().measurementKind())
            );
        };
    }

    private AggregateKind aggregateKind() {
        String aggregate = meterReading().readingType().aggregate();
        return AggregateKind.valueOf(aggregate.toUpperCase(Locale.ROOT));
    }

    private TimeSeriesComplexType.SeriesPeriodList seriesPeriods(boolean convertToKiloWatt) {
        var interval = new EsmpTimeInterval(
                meterReading().start().format(ENEDIS_DATE_FORMAT),
                meterReading().end().format(ENEDIS_DATE_FORMAT),
                ENEDIS_DATE_FORMAT,
                ECT
        );
        var meterReading = meterReading();
        String resolution = identifiableMeterReading.permissionRequest().granularity().name();
        List<PointComplexType> points = new ArrayList<>();
        int position = 0;
        for (IntervalReading intervalReading
                : meterReading.intervalReadings()) {

            var quantity = Double.parseDouble(intervalReading.value());
            if (convertToKiloWatt) {
                quantity = quantity / 1000.0;
            }
            PointComplexType point = new PointComplexType()
                    .withPosition("%d".formatted(position))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                    .withEnergyQuantityQuality(QualityTypeList.AS_PROVIDED);
            points.add(point);
            position++;
        }

        var seriesPeriod = new SeriesPeriodComplexType()
                .withResolution(resolution)
                .withTimeInterval(new ESMPDateTimeIntervalComplexType()
                                          .withStart(interval.start())
                                          .withEnd(interval.end())
                )
                .withPointList(
                        new SeriesPeriodComplexType.PointList()
                                .withPoints(points)
                );
        return new TimeSeriesComplexType.SeriesPeriodList()
                .withSeriesPeriods(List.of(seriesPeriod));
    }
}
