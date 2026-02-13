package energy.eddie.regionconnector.de.eta.providers.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.dtos.EtaMeteringDataPayload;
import energy.eddie.regionconnector.de.eta.dtos.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Intermediate document for mapping ETA Plus metering data to CIM v1.04 VHD format.
 * This class handles the conversion from the ETA Plus specific data format to the
 * standardized CIM Validated Historical Data Market Document.
 */
class IntermediateValidatedHistoricalDataMarketDocument {
    private final PlainDeConfiguration deConfiguration;
    private final IdentifiableValidatedHistoricalData identifiableData;
    private final Granularity granularity;
    private final DataNeedsService dataNeedsService;

    IntermediateValidatedHistoricalDataMarketDocument(
            PlainDeConfiguration deConfiguration,
            IdentifiableValidatedHistoricalData identifiableData,
            DataNeedsService dataNeedsService
    ) {
        this.deConfiguration = deConfiguration;
        this.identifiableData = identifiableData;
        this.granularity = identifiableData.permissionRequest().granularity();
        this.dataNeedsService = dataNeedsService;
    }

    /**
     * Convert the metering data to a list of VHD envelopes.
     *
     * @return List of VHDEnvelope documents
     */
    public List<VHDEnvelope> toVHD() {
        EtaMeteringDataPayload payload = identifiableData.payload();
        ValidatedHistoricalDataDataNeed dataNeed = (ValidatedHistoricalDataDataNeed) dataNeedsService
                .getById(identifiableData.permissionRequest().dataNeedId());

        ZonedDateTime fetchTime = payload.fetchTime();
        EnergyType energyType = dataNeed.energyType();

        return createVHDDocuments(payload, fetchTime, energyType);
    }

    private List<VHDEnvelope> createVHDDocuments(
            EtaMeteringDataPayload payload,
            @Nullable ZonedDateTime fetchTime,
            EnergyType energyType
    ) {
        List<VHDEnvelope> vhdEnvelopes = new ArrayList<>();

        // Calculate time interval from readings
        ESMPDateTimeInterval periodInterval = calculatePeriodInterval(payload.readings());

        // Create time series from readings
        List<TimeSeries> timeSeriesList = createTimeSeries(payload, energyType);

        // Create the VHD Market Document
        VHDMarketDocument vhdDocument = createVHDMarketDocument(
                fetchTime,
                periodInterval,
                timeSeriesList
        );

        // Wrap in envelope
        vhdEnvelopes.add(new VhdEnvelopeWrapper(vhdDocument, identifiableData.permissionRequest()).wrap());

        return vhdEnvelopes;
    }

    private VHDMarketDocument createVHDMarketDocument(
            @Nullable ZonedDateTime fetchTime,
            ESMPDateTimeInterval periodInterval,
            List<TimeSeries> timeSeriesList
    ) {
        fetchTime = fetchTime == null ? ZonedDateTime.now(ZoneOffset.UTC) : fetchTime;

        return new VHDMarketDocument()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V1_04.cimify())
                .withType(StandardMessageTypeList.MEASUREMENT_VALUE_DOCUMENT.value())
                .withSenderMarketParticipantMarketRoleType(StandardRoleTypeList.METERING_POINT_ADMINISTRATOR.value())
                .withReceiverMarketParticipantMarketRoleType(StandardRoleTypeList.CONSUMER.value())
                .withProcessProcessType(StandardProcessTypeList.REALISED.value())
                .withSenderMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY.value())
                                .withValue("ETA-Plus")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.GERMANY.value())
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withPeriodTimeInterval(periodInterval)
                .withCreatedDateTime(fetchTime)
                .withTimeSeries(timeSeriesList);
    }

    private List<TimeSeries> createTimeSeries(EtaMeteringDataPayload payload, EnergyType energyType) {
        List<TimeSeries> timeSeriesList = new ArrayList<>();

        if (payload.readings() == null || payload.readings().isEmpty()) {
            return timeSeriesList;
        }

        // Group readings by unit
        Map<String, List<EtaMeteringDataPayload.MeterReading>> readingsByUnit = new HashMap<>();
        for (EtaMeteringDataPayload.MeterReading reading : payload.readings()) {
            readingsByUnit
                    .computeIfAbsent(reading.unit(), k -> new ArrayList<>())
                    .add(reading);
        }

        for (Map.Entry<String, List<EtaMeteringDataPayload.MeterReading>> entry : readingsByUnit.entrySet()) {
            String unit = entry.getKey();
            List<EtaMeteringDataPayload.MeterReading> readings = entry.getValue();

            // Create points from readings
            List<Point> points = createPoints(readings);

            // Create series period
            SeriesPeriod seriesPeriod = createSeriesPeriod(readings, points);

            // Create time series
            TimeSeries timeSeries = new TimeSeries()
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(getBusinessType(energyType).value())
                    .withVersion("1")
                    .withProduct(getEnergyProduct(energyType).value())
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(getCommodityKind(energyType))
                    .withFlowDirectionDirection(StandardDirectionTypeList.DOWN.value())
                    .withEnergyMeasurementUnitName(mapUnitToCim(unit).value())
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDString()
                                    .withCodingScheme(StandardCodingSchemeTypeList.GERMANY.value())
                                    .withValue(payload.meteringPointId())
                    )
                    .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                    .withPeriods(seriesPeriod);

            timeSeriesList.add(timeSeries);
        }

        return timeSeriesList;
    }

    private List<Point> createPoints(List<EtaMeteringDataPayload.MeterReading> readings) {
        List<Point> points = new ArrayList<>();

        // Sort readings by timestamp
        List<EtaMeteringDataPayload.MeterReading> sortedReadings = new ArrayList<>(readings);
        sortedReadings.sort(Comparator.comparing(EtaMeteringDataPayload.MeterReading::timestamp));

        int position = 1;
        for (EtaMeteringDataPayload.MeterReading reading : sortedReadings) {
            Point point = new Point()
                    .withPosition(position++)
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(reading.value()))
                    .withEnergyQuantityQuality(mapQualityToCim(reading.quality()).value());
            points.add(point);
        }

        return points;
    }

    private SeriesPeriod createSeriesPeriod(
            List<EtaMeteringDataPayload.MeterReading> readings,
            List<Point> points
    ) {
        // Calculate time interval from readings
        ZonedDateTime start = readings.stream()
                .map(EtaMeteringDataPayload.MeterReading::timestamp)
                .min(ZonedDateTime::compareTo)
                .orElse(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID));

        ZonedDateTime end = readings.stream()
                .map(EtaMeteringDataPayload.MeterReading::timestamp)
                .max(ZonedDateTime::compareTo)
                .orElse(ZonedDateTime.now(EtaRegionConnectorMetadata.DE_ZONE_ID));

        EsmpTimeInterval interval = new EsmpTimeInterval(start, end);

        return new SeriesPeriod()
                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(granularity.duration().toMillis()))
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPoints(points);
    }

    private ESMPDateTimeInterval calculatePeriodInterval(List<EtaMeteringDataPayload.MeterReading> readings) {
        if (readings == null || readings.isEmpty()) {
            return new ESMPDateTimeInterval();
        }

        ZonedDateTime start = readings.stream()
                .map(EtaMeteringDataPayload.MeterReading::timestamp)
                .min(ZonedDateTime::compareTo)
                .orElse(null);

        ZonedDateTime end = readings.stream()
                .map(EtaMeteringDataPayload.MeterReading::timestamp)
                .max(ZonedDateTime::compareTo)
                .orElse(null);

        if (start == null || end == null) {
            return new ESMPDateTimeInterval();
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeInterval()
                .withStart(interval.start())
                .withEnd(interval.end());
    }

    private CommodityKind getCommodityKind(EnergyType energyType) {
        return switch (energyType) {
            case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case NATURAL_GAS -> CommodityKind.NATURALGAS;
            default -> throw new IllegalStateException("Unexpected energy type: " + energyType);
        };
    }

    private StandardBusinessTypeList getBusinessType(EnergyType energyType) {
        // Default to consumption for validated historical data
        return StandardBusinessTypeList.CONSUMPTION;
    }

    private StandardEnergyProductTypeList getEnergyProduct(EnergyType energyType) {
        // Active energy is used for all energy types in validated historical data
        return StandardEnergyProductTypeList.ACTIVE_ENERGY;
    }

    private StandardUnitOfMeasureTypeList mapUnitToCim(String unit) {
        if (unit == null) {
            return StandardUnitOfMeasureTypeList.KWH;
        }
        return switch (unit.toUpperCase(Locale.ROOT)) {
            case "KWH" -> StandardUnitOfMeasureTypeList.KWH;
            case "MWH" -> StandardUnitOfMeasureTypeList.MWH;
            case "WH" -> StandardUnitOfMeasureTypeList.WH;
            case "M3" -> StandardUnitOfMeasureTypeList.CUBIC_METRE;
            default -> StandardUnitOfMeasureTypeList.KWH;
        };
    }

    private StandardQualityTypeList mapQualityToCim(EtaMeteringDataPayload.QualityIndicator quality) {
        return switch (quality) {
            case MEASURED -> StandardQualityTypeList.AS_PROVIDED;
            case ESTIMATED -> StandardQualityTypeList.ESTIMATED;
            case SUBSTITUTED -> StandardQualityTypeList.SUBSTITUTED;
        };
    }
}

