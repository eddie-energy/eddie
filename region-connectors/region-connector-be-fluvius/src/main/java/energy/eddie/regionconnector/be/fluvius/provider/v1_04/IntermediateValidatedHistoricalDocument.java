package energy.eddie.regionconnector.be.fluvius.provider.v1_04;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.EnergyProductTypeList;
import energy.eddie.cim.v1_04.*;
import energy.eddie.cim.v1_04.vhd.*;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v1_04.VhdEnvelopeWrapper;
import jakarta.annotation.Nullable;

import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

class IntermediateValidatedHistoricalDocument {
    private final FluviusOAuthConfiguration fluviusConfig;
    private final IdentifiableMeteringData identifiableMeteredData;
    private final Granularity granularity;
    private final DataNeedsService dataNeedsService;

    IntermediateValidatedHistoricalDocument(
            FluviusOAuthConfiguration fluviusConfiguration,
            IdentifiableMeteringData identifiableMeteredData,
            DataNeedsService dataNeedsService
    ) {
        this.fluviusConfig = fluviusConfiguration;
        this.granularity = identifiableMeteredData.permissionRequest().granularity();
        this.identifiableMeteredData = identifiableMeteredData;
        this.dataNeedsService = dataNeedsService;
    }

    public List<VHDEnvelope> toVHD() {
        GetEnergyResponseModel meteringData = identifiableMeteredData.payload().data();
        ValidatedHistoricalDataDataNeed dataNeed = (ValidatedHistoricalDataDataNeed) dataNeedsService.
                getById(identifiableMeteredData.permissionRequest().dataNeedId());

        ZonedDateTime fetchTime = meteringData.fetchTime();
        EnergyType type = dataNeed.energyType();
        return getVHD(meteringData, fetchTime, type);
    }

    private List<VHDEnvelope> getVHD(
            GetEnergyResponseModel response,
            @Nullable ZonedDateTime fetchTime,
            EnergyType energyType
    ) {
        var meteringData = response.getMeterFor(energyType);
        List<VHDEnvelope> vhds = new ArrayList<>();
        for (MeterResponseModel meterResponse : meteringData) {
            int seqNumber = meterResponse.seqNumber();
            ESMPDateTimeInterval periodIntervalTime = getPeriodTimeInterval(meterResponse.getByGranularity(granularity));
            List<TimeSeries> timeSeries = getTimeSeries(
                    meterResponse.getByGranularity(granularity),
                    commodityKind(energyType),
                    getFlowDirection(meterResponse.hasOfftake(granularity), meterResponse.hasInjection(granularity)),
                    meterResponse.meterID()
            );

            VHDMarketDocument vhd = createVHD(fetchTime, seqNumber, periodIntervalTime, timeSeries);
            vhds.add(new VhdEnvelopeWrapper(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }

        return vhds;
    }

    private CommodityKind commodityKind(EnergyType energyType) {
        return switch (energyType) {
            case ELECTRICITY -> CommodityKind.ELECTRICITYPRIMARYMETERED;
            case NATURAL_GAS -> CommodityKind.NATURALGAS;
            default -> throw new IllegalStateException("Unexpected value: " + energyType);
        };
    }

    private VHDMarketDocument createVHD(
            @Nullable ZonedDateTime fetchTime,
            int seqNumber,
            ESMPDateTimeInterval periodIntervalTime,
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
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue("Fluvius")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDString()
                                .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                .withValue(fluviusConfig.clientId())
                )
                .withDescription(String.valueOf(seqNumber))
                .withPeriodTimeInterval(periodIntervalTime)
                .withCreatedDateTime(fetchTime)
                .withTimeSeries(timeSeriesList);
    }

    private List<TimeSeries> createTimeSeriesList(
            Map<String, SeriesPeriod> unitSeriesPeriodMap,
            CommodityKind type,
            StandardDirectionTypeList flowDirection,
            @Nullable String meterId
    ) {
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        var standardBusinessType = getBusinessType(flowDirection);
        var businessType = standardBusinessType == null ? null : standardBusinessType.value();
        var metaData = identifiableMeteredData.payload().metaData();
        var version = metaData == null ? "1" : metaData.version();
        for (var entry : unitSeriesPeriodMap.entrySet()) {
            timeSeriesList.add(
                    new TimeSeries()
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(businessType)
                            .withVersion(version)
                            .withProduct(EnergyProductTypeList.ACTIVE_ENERGY.value())
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(type)
                            .withFlowDirectionDirection(flowDirection.value())
                            .withEnergyMeasurementUnitName(getUnitCIM(entry.getKey()).value())
                            .withMarketEvaluationPointMRID(
                                    new MeasurementPointIDString()
                                            .withCodingScheme(StandardCodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME.value())
                                            .withValue(meterId)
                            )
                            .withReasonCode(StandardReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED.value())
                            .withPeriods(entry.getValue())
            );
        }

        return timeSeriesList;
    }

    private StandardUnitOfMeasureTypeList getUnitCIM(String unit) {
        return unit.equalsIgnoreCase("m3")
                ? StandardUnitOfMeasureTypeList.CUBIC_METRE
                : StandardUnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private List<TimeSeries> getTimeSeries(
            @Nullable List<? extends EnergyItemResponseModel<?>> energyItems,
            CommodityKind commodityKind,
            StandardDirectionTypeList direction,
            @Nullable String meterId
    ) {
        if (energyItems == null) {
            return List.of();
        }

        List<TimeSeries> timeSeriesList = new ArrayList<>();

        for (EnergyItemResponseModel<?> item : energyItems) {
            Map<String, List<Point>> unitPointsMap = getPoints(item);
            Map<String, SeriesPeriod> unitSeriesPeriodMap = getSeriesPeriods(
                    item.timestampStart(),
                    item.timestampEnd(),
                    unitPointsMap
            );
            timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, commodityKind, direction, meterId));
        }

        return timeSeriesList;
    }

    private Map<String, SeriesPeriod> getSeriesPeriods(
            ZonedDateTime timestampStart,
            ZonedDateTime timestampEnd,
            Map<String, List<Point>> unitPointsMap
    ) {
        Map<String, SeriesPeriod> seriesPeriodMap = new HashMap<>();
        for (Map.Entry<String, List<Point>> entry : unitPointsMap.entrySet()) {
            seriesPeriodMap.put(entry.getKey(), createSeriesPeriod(timestampStart, timestampEnd, entry.getValue()));
        }

        return seriesPeriodMap;
    }

    private Map<String, List<Point>> getPoints(EnergyItemResponseModel<?> response) {
        List<? extends MeasurementResponseModel> measurements = response.measurement();
        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<Point>> pointsMap = new HashMap<>();

        for (int i = 0; i < measurements.size(); i++) {
            MeasurementResponseModel measurement = measurements.get(i);
            String unit = measurement.unit();
            Double quantity = measurement.value();
            StandardQualityTypeList quality = getQualityType(measurement.validationStates());

            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(i + 1, quantity, quality));
        }

        return pointsMap;
    }

    private Point createPoint(int position, Double quantity, StandardQualityTypeList quality) {
        return new Point()
                .withPosition(position)
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(quality == null ? null : quality.value());
    }

    private StandardQualityTypeList getQualityType(ValidationState... validationStates) {
        for (ValidationState state : validationStates) {
            if (state == ValidationState.EST) {
                return StandardQualityTypeList.ESTIMATED;
            }
        }

        return StandardQualityTypeList.AS_PROVIDED;
    }

    private SeriesPeriod createSeriesPeriod(
            ZonedDateTime timestampStart,
            ZonedDateTime timestampEnd,
            List<Point> points
    ) {
        EsmpTimeInterval interval = new EsmpTimeInterval(timestampStart, timestampEnd);
        return new SeriesPeriod()
                .withResolution(DatatypeFactory.newDefaultInstance().newDuration(granularity.duration().toMillis()))
                .withTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPoints(points);
    }

    private StandardDirectionTypeList getFlowDirection(boolean hasOfftake, boolean hasInjection) {
        if (hasOfftake) {
            return hasInjection ? StandardDirectionTypeList.UP_AND_DOWN : StandardDirectionTypeList.DOWN;
        } else {
            return StandardDirectionTypeList.UP;
        }
    }

    @Nullable
    private StandardBusinessTypeList getBusinessType(StandardDirectionTypeList direction) {
        return switch (direction) {
            case UP -> StandardBusinessTypeList.PRODUCTION;
            case DOWN -> StandardBusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            default -> null;
        };
    }

    @SuppressWarnings({"NullAway", "javabugs:S2259"})
    // False positive for items, since it is checked in a separate method
    private ESMPDateTimeInterval getPeriodTimeInterval(@Nullable List<? extends EnergyItemResponseModel<?>> items) {
        ESMPDateTimeInterval periodTimeInterval = new ESMPDateTimeInterval();

        if (isNullOrEmpty(items)) {
            return periodTimeInterval;
        }

        ZonedDateTime earliestTimestampStart = items.getFirst().timestampStart();
        ZonedDateTime latestTimestampEnd = items.getFirst().timestampEnd();

        for (EnergyItemResponseModel<?> item : items) {
            earliestTimestampStart = getEarliestTimestamp(earliestTimestampStart, item.timestampStart());
            latestTimestampEnd = getLatestTimestamp(latestTimestampEnd, item.timestampEnd());
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart, latestTimestampEnd);
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }


    private static boolean isNullOrEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }

    private static ZonedDateTime getLatestTimestamp(ZonedDateTime current, ZonedDateTime other) {
        return other.isAfter(current) ? other : current;
    }

    private static ZonedDateTime getEarliestTimestamp(ZonedDateTime current, ZonedDateTime other) {
        return other.isBefore(current) ? other : current;
    }
}
