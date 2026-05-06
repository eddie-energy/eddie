package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusVhdMappings;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps a single ETA Plus historical-readings response to a CIM v0.82
 * {@link ValidatedHistoricalDataEnvelope} for backwards compatibility.
 *
 * <p>Same response shape as the v1.04 mapping: one MP, one unit, one direction, one TimeSeries.
 */
class IntermediateValidatedHistoricalDataEnvelope {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateValidatedHistoricalDataEnvelope.class);

    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;
    private final IdentifiableValidatedHistoricalData identifiableData;

    IntermediateValidatedHistoricalDataEnvelope(
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration,
            IdentifiableValidatedHistoricalData identifiableData
    ) {
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
        this.identifiableData = identifiableData;
    }

    public List<ValidatedHistoricalDataEnvelope> toVhd() {
        var payload = identifiableData.payload();
        var readings = payload.readings();
        if (readings == null || readings.isEmpty()) {
            return List.of();
        }

        var firstReading = readings.get(0);
        UnitOfMeasureTypeList unit;
        try {
            unit = translateUnit(firstReading.unit());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Skipping VHD for metering point {}: unsupported unit '{}'",
                    payload.meteringPointId(), firstReading.unit());
            return List.of();
        }

        var sortedReadings = EtaPlusVhdMappings.sortByTimestamp(readings);
        var start = sortedReadings.get(0).timestamp();
        var end = sortedReadings.get(sortedReadings.size() - 1).timestamp();

        var permissionRequest = identifiableData.permissionRequest();
        var energyType = permissionRequest.energyType();
        var direction = firstReading.direction();
        var senderId = permissionRequest.dataSourceInformation().meteredDataAdministratorId();

        var vhd = new ValidatedHistoricalDataMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withCreatedDateTime(new EsmpDateTime(ZonedDateTime.now(ZoneOffset.UTC)).toString())
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                .withValue(senderId)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme())
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withPeriodTimeInterval(esmpInterval(start, end))
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(timeSeries(payload, sortedReadings, energyType, unit, direction, start, end))
                );

        return List.of(new VhdEnvelope(vhd, permissionRequest).wrap());
    }

    private TimeSeriesComplexType timeSeries(
            EtaPlusMeteredData payload,
            List<EtaPlusMeteredData.MeterReading> sortedReadings,
            EnergyType energyType,
            UnitOfMeasureTypeList unit,
            String direction,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var ts = new TimeSeriesComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withVersion("1")
                .withBusinessType(businessTypeFor(direction))
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(commodityKindFor(energyType))
                .withFlowDirectionDirection(flowDirectionFor(direction))
                .withEnergyMeasurementUnitName(unit)
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                .withValue(payload.meteringPointId())
                )
                .withReasonList(
                        new TimeSeriesComplexType.ReasonList()
                                .withReasons(new ReasonComplexType()
                                        .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED))
                )
                .withSeriesPeriodList(
                        new TimeSeriesComplexType.SeriesPeriodList()
                                .withSeriesPeriods(seriesPeriod(sortedReadings, start, end))
                );

        // Q-2 (L1): set product only for electricity; CIM v0.82 has no gas-appropriate value.
        var product = productFor(energyType);
        if (product != null) {
            ts.withProduct(product);
        }
        return ts;
    }

    private SeriesPeriodComplexType seriesPeriod(
            List<EtaPlusMeteredData.MeterReading> sortedReadings,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var granularity = identifiableData.permissionRequest().granularity();

        var points = new ArrayList<PointComplexType>(sortedReadings.size());
        for (int i = 0; i < sortedReadings.size(); i++) {
            var reading = sortedReadings.get(i);
            var point = new PointComplexType()
                    .withPosition(String.valueOf(i + 1))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(reading.value()));
            var quality = qualityFor(reading.quality());
            if (quality != null) {
                point.withEnergyQuantityQuality(quality);
            }
            points.add(point);
        }

        return new SeriesPeriodComplexType()
                .withResolution(granularity.toString())
                .withTimeInterval(esmpInterval(start, end))
                .withPointList(new SeriesPeriodComplexType.PointList().withPoints(points))
                .withReasonList(new SeriesPeriodComplexType.ReasonList());
    }

    private static ESMPDateTimeIntervalComplexType esmpInterval(ZonedDateTime start, ZonedDateTime end) {
        var interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeIntervalComplexType()
                .withStart(interval.start())
                .withEnd(interval.end());
    }

    private CommodityKind commodityKindFor(EnergyType energyType) {
        return switch (energyType) {
            case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case NATURAL_GAS -> CommodityKind.NATURALGAS;
            default -> throw new IllegalStateException("Unsupported energy type: " + energyType);
        };
    }

    private BusinessTypeList businessTypeFor(String direction) {
        if (EtaPlusVhdMappings.isProduction(direction)) {
            return BusinessTypeList.PRODUCTION;
        }
        if (!"Consumption".equals(direction)) {
            LOGGER.warn("Unknown direction '{}' on metering point {}; defaulting to CONSUMPTION",
                    direction, identifiableData.payload().meteringPointId());
        }
        return BusinessTypeList.CONSUMPTION;
    }

    private static DirectionTypeList flowDirectionFor(String direction) {
        return EtaPlusVhdMappings.isProduction(direction) ? DirectionTypeList.UP : DirectionTypeList.DOWN;
    }

    @Nullable
    private static EnergyProductTypeList productFor(EnergyType energyType) {
        return energyType == EnergyType.ELECTRICITY ? EnergyProductTypeList.ACTIVE_ENERGY : null;
    }

    @Nullable
    private QualityTypeList qualityFor(String wireStatus) {
        if (EtaPlusVhdMappings.isValidatedStatus(wireStatus)) {
            return QualityTypeList.AS_PROVIDED;
        }
        LOGGER.info("Unknown reading status '{}' on metering point {}; omitting quality",
                wireStatus, identifiableData.payload().meteringPointId());
        return null;
    }

    private static UnitOfMeasureTypeList translateUnit(String wireUnit) {
        if (wireUnit == null) {
            throw new IllegalArgumentException("null");
        }
        return switch (wireUnit) {
            case "kWh", "KWH" -> UnitOfMeasureTypeList.KILOWATT_HOUR;
            case "MWh", "MWH" -> UnitOfMeasureTypeList.MEGAWATT_HOURS;
            case "m³", "m3", "M3" -> UnitOfMeasureTypeList.CUBIC_METRE;
            default -> throw new IllegalArgumentException(wireUnit);
        };
    }
}