package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.api.cim.config.CommonInformationModelConfiguration;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.regionconnector.de.eta.config.DeEtaPlusConfiguration;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusMeteredData;
import energy.eddie.regionconnector.de.eta.providers.EtaPlusVhdMappings;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Maps a single ETA Plus historical-readings response (one metering point, one unit, one direction)
 * to a CIM v1.04 {@link VHDMarketDocument} wrapped in a {@link VHDEnvelope}.
 *
 * <p>Per the ETA Plus contract, {@code unit} and {@code direction} are constant within a response,
 * so each response yields exactly one {@link TimeSeries}.
 */
class IntermediateValidatedHistoricalDataMarketDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateValidatedHistoricalDataMarketDocument.class);

    private final CommonInformationModelConfiguration cimConfig;
    private final DeEtaPlusConfiguration deConfiguration;
    private final IdentifiableValidatedHistoricalData identifiableData;

    IntermediateValidatedHistoricalDataMarketDocument(
            CommonInformationModelConfiguration cimConfig,
            DeEtaPlusConfiguration deConfiguration,
            IdentifiableValidatedHistoricalData identifiableData
    ) {
        this.cimConfig = cimConfig;
        this.deConfiguration = deConfiguration;
        this.identifiableData = identifiableData;
    }

    public List<VHDEnvelope> toVhd() {
        var payload = identifiableData.payload();
        var readings = payload.readings();
        if (readings == null || readings.isEmpty()) {
            return List.of();
        }

        var firstReading = readings.get(0);
        StandardUnitOfMeasureTypeList unit;
        try {
            unit = StandardUnitOfMeasureTypeList.fromValue(EtaPlusVhdMappings.translateUnit(firstReading.unit()));
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

        var vhd = new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withCreatedDateTime(ZonedDateTime.now(ZoneOffset.UTC))
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME.value())
                                .withValue(senderId)
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(cimConfig.eligiblePartyNationalCodingScheme().value())
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withPeriodTimeInterval(esmpInterval(start, end))
                .withTimeSeries(timeSeries(payload, sortedReadings, energyType, unit, direction, start, end));

        return List.of(new VhdEnvelopeWrapper(vhd, permissionRequest).wrap());
    }

    private TimeSeries timeSeries(
            EtaPlusMeteredData payload,
            List<EtaPlusMeteredData.MeterReading> sortedReadings,
            EnergyType energyType,
            StandardUnitOfMeasureTypeList unit,
            String direction,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var ts = new TimeSeries()
                .withMRID(UUID.randomUUID().toString())
                .withVersion("1")
                .withBusinessType(businessTypeFor(direction).value())
                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(commodityKindFor(energyType))
                .withFlowDirectionDirection(flowDirectionFor(direction).value())
                .withEnergyMeasurementUnitName(unit.value())
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME.value())
                                .withValue(payload.meteringPointId())
                )
                .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                .withPeriods(seriesPeriod(sortedReadings, start, end));

        // Q-2 (L1): set product only for electricity; CIM v1.04 has no gas-appropriate value.
        var product = productFor(energyType);
        if (product != null) {
            ts.withProduct(product.value());
        }
        return ts;
    }

    private SeriesPeriod seriesPeriod(
            List<EtaPlusMeteredData.MeterReading> sortedReadings,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var granularity = identifiableData.permissionRequest().granularity();
        var resolution = DatatypeFactory.newDefaultInstance().newDuration(granularity.duration().toMillis());

        var points = new java.util.ArrayList<Point>(sortedReadings.size());
        for (int i = 0; i < sortedReadings.size(); i++) {
            var reading = sortedReadings.get(i);
            var point = new Point()
                    .withPosition(i + 1)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(reading.value()));
            var quality = qualityFor(reading.quality());
            if (quality != null) {
                point.withEnergyQuantityQuality(quality.value());
            }
            points.add(point);
        }

        return new SeriesPeriod()
                .withResolution(resolution)
                .withTimeInterval(esmpInterval(start, end))
                .withPoints(points);
    }

    private static ESMPDateTimeInterval esmpInterval(ZonedDateTime start, ZonedDateTime end) {
        var interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeInterval()
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

    private StandardBusinessTypeList businessTypeFor(String direction) {
        if (EtaPlusVhdMappings.isProduction(direction)) {
            return StandardBusinessTypeList.PRODUCTION;
        }
        if (!"Consumption".equals(direction)) {
            LOGGER.warn("Unknown direction '{}' on metering point {}; defaulting to CONSUMPTION",
                    direction, identifiableData.payload().meteringPointId());
        }
        return StandardBusinessTypeList.CONSUMPTION;
    }

    private static StandardDirectionTypeList flowDirectionFor(String direction) {
        return EtaPlusVhdMappings.isProduction(direction)
                ? StandardDirectionTypeList.UP
                : StandardDirectionTypeList.DOWN;
    }

    @Nullable
    private static StandardEnergyProductTypeList productFor(EnergyType energyType) {
        return energyType == EnergyType.ELECTRICITY ? StandardEnergyProductTypeList.ACTIVE_ENERGY : null;
    }

    @Nullable
    private StandardQualityTypeList qualityFor(String wireStatus) {
        if (EtaPlusVhdMappings.isValidatedStatus(wireStatus)) {
            return StandardQualityTypeList.AS_PROVIDED;
        }
        LOGGER.info("Unknown reading status '{}' on metering point {}; omitting quality",
                wireStatus, identifiableData.payload().meteringPointId());
        return null;
    }
}