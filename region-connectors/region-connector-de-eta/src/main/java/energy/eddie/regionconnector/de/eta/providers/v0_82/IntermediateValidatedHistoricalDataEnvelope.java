package energy.eddie.regionconnector.de.eta.providers.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.de.eta.EtaRegionConnectorMetadata;
import energy.eddie.regionconnector.de.eta.config.PlainDeConfiguration;
import energy.eddie.regionconnector.de.eta.dtos.EtaMeteringDataPayload;
import energy.eddie.regionconnector.de.eta.dtos.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Intermediate document for mapping ETA Plus metering data to CIM v0.82 VHD format.
 * This class handles the conversion from the ETA Plus specific data format to the
 * CIM v0.82 Validated Historical Data Envelope for backwards compatibility.
 */
class IntermediateValidatedHistoricalDataEnvelope {
    private final PlainDeConfiguration deConfiguration;
    private final IdentifiableValidatedHistoricalData identifiableData;
    private final Granularity granularity;
    private final DataNeedsService dataNeedsService;

    IntermediateValidatedHistoricalDataEnvelope(
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
     * @return List of ValidatedHistoricalDataEnvelope documents
     */
    public List<ValidatedHistoricalDataEnvelope> toVHD() {
        EtaMeteringDataPayload payload = identifiableData.payload();
        ValidatedHistoricalDataDataNeed dataNeed = (ValidatedHistoricalDataDataNeed) dataNeedsService
                .getById(identifiableData.permissionRequest().dataNeedId());

        ZonedDateTime fetchTime = payload.fetchTime();
        EnergyType energyType = dataNeed.energyType();

        return createVHDDocuments(payload, fetchTime, energyType);
    }

    private List<ValidatedHistoricalDataEnvelope> createVHDDocuments(
            EtaMeteringDataPayload payload,
            @Nullable ZonedDateTime fetchTime,
            EnergyType energyType
    ) {
        List<ValidatedHistoricalDataEnvelope> vhdEnvelopes = new ArrayList<>();

        // Calculate time interval from readings
        ESMPDateTimeIntervalComplexType periodInterval = calculatePeriodInterval(payload.readings());

        // Create time series from readings
        List<TimeSeriesComplexType> timeSeriesList = createTimeSeries(payload, energyType);

        // Create the VHD Market Document
        ValidatedHistoricalDataMarketDocumentComplexType vhdDocument = createVHDMarketDocument(
                fetchTime,
                periodInterval,
                timeSeriesList
        );

        // Wrap in envelope
        vhdEnvelopes.add(new VhdEnvelope(vhdDocument, identifiableData.permissionRequest()).wrap());

        return vhdEnvelopes;
    }

    private ValidatedHistoricalDataMarketDocumentComplexType createVHDMarketDocument(
            @Nullable ZonedDateTime fetchTime,
            ESMPDateTimeIntervalComplexType periodInterval,
            List<TimeSeriesComplexType> timeSeriesList
    ) {
        fetchTime = fetchTime == null ? ZonedDateTime.now(ZoneOffset.UTC) : fetchTime;

        return new ValidatedHistoricalDataMarketDocumentComplexType()
                .withMRID(UUID.randomUUID().toString())
                .withRevisionNumber(CommonInformationModelVersions.V0_82.version())
                .withType(MessageTypeList.MEASUREMENT_VALUE_DOCUMENT)
                .withSenderMarketParticipantMarketRoleType(RoleTypeList.METERING_POINT_ADMINISTRATOR)
                .withReceiverMarketParticipantMarketRoleType(RoleTypeList.CONSUMER)
                .withProcessProcessType(ProcessTypeList.REALISED)
                .withSenderMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                .withValue("ETA-Plus")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                .withValue(deConfiguration.eligiblePartyId())
                )
                .withPeriodTimeInterval(periodInterval)
                .withCreatedDateTime(new EsmpDateTime(fetchTime).toString())
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(timeSeriesList)
                );
    }

    private List<TimeSeriesComplexType> createTimeSeries(EtaMeteringDataPayload payload, EnergyType energyType) {
        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();

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
            List<PointComplexType> points = createPoints(readings);

            // Create series period
            SeriesPeriodComplexType seriesPeriod = createSeriesPeriod(readings, points);

            // Create time series
            TimeSeriesComplexType timeSeries = new TimeSeriesComplexType()
                    .withMRID(UUID.randomUUID().toString())
                    .withBusinessType(getBusinessType(energyType))
                    .withProduct(getEnergyProduct(energyType))
                    .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(getCommodityKind(energyType))
                    .withFlowDirectionDirection(DirectionTypeList.DOWN)
                    .withEnergyMeasurementUnitName(mapUnitToCim(unit))
                    .withMarketEvaluationPointMRID(
                            new MeasurementPointIDStringComplexType()
                                    .withCodingScheme(CodingSchemeTypeList.GERMANY_NATIONAL_CODING_SCHEME)
                                    .withValue(payload.meteringPointId())
                    )
                    .withReasonList(new TimeSeriesComplexType.ReasonList()
                            .withReasons(
                                    new ReasonComplexType()
                                            .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                            )
                    )
                    .withSeriesPeriodList(
                            new TimeSeriesComplexType.SeriesPeriodList()
                                    .withSeriesPeriods(seriesPeriod)
                    );

            timeSeriesList.add(timeSeries);
        }

        return timeSeriesList;
    }

    private List<PointComplexType> createPoints(List<EtaMeteringDataPayload.MeterReading> readings) {
        List<PointComplexType> points = new ArrayList<>();

        // Sort readings by timestamp
        List<EtaMeteringDataPayload.MeterReading> sortedReadings = new ArrayList<>(readings);
        sortedReadings.sort(Comparator.comparing(EtaMeteringDataPayload.MeterReading::timestamp));

        int position = 1;
        for (EtaMeteringDataPayload.MeterReading reading : sortedReadings) {
            PointComplexType point = new PointComplexType()
                    .withPosition(String.valueOf(position++))
                    .withEnergyQuantityQuantity(BigDecimal.valueOf(reading.value()))
                    .withEnergyQuantityQuality(mapQualityToCim(reading.quality()));
            points.add(point);
        }

        return points;
    }

    private SeriesPeriodComplexType createSeriesPeriod(
            List<EtaMeteringDataPayload.MeterReading> readings,
            List<PointComplexType> points
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

        return new SeriesPeriodComplexType()
                .withResolution(granularity.toString())
                .withTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));
    }

    private ESMPDateTimeIntervalComplexType calculatePeriodInterval(List<EtaMeteringDataPayload.MeterReading> readings) {
        if (readings == null || readings.isEmpty()) {
            return new ESMPDateTimeIntervalComplexType();
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
            return new ESMPDateTimeIntervalComplexType();
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(start, end);
        return new ESMPDateTimeIntervalComplexType()
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

    private BusinessTypeList getBusinessType(EnergyType energyType) {
        // Default to consumption for validated historical data
        return BusinessTypeList.CONSUMPTION;
    }

    private EnergyProductTypeList getEnergyProduct(EnergyType energyType) {
        return EnergyProductTypeList.ACTIVE_ENERGY;
    }

    private UnitOfMeasureTypeList mapUnitToCim(String unit) {
        if (unit == null) {
            return UnitOfMeasureTypeList.KWH;
        }
        return switch (unit.toUpperCase(Locale.ROOT)) {
            case "KWH" -> UnitOfMeasureTypeList.KWH;
            case "MWH" -> UnitOfMeasureTypeList.MWH;
            case "WH" -> UnitOfMeasureTypeList.WH;
            case "M3" -> UnitOfMeasureTypeList.CUBIC_METRE;
            default -> UnitOfMeasureTypeList.KWH;
        };
    }

    private QualityTypeList mapQualityToCim(EtaMeteringDataPayload.QualityIndicator quality) {
        return switch (quality) {
            case MEASURED -> QualityTypeList.AS_PROVIDED;
            case ESTIMATED -> QualityTypeList.ESTIMATED;
            case SUBSTITUTED -> QualityTypeList.SUBSTITUTED;
        };
    }
}

