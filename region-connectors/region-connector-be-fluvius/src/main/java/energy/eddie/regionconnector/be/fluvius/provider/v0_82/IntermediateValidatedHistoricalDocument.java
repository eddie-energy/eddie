package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.CommonInformationModelVersions;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

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

    public List<ValidatedHistoricalDataEnvelope> toVHD() {
        GetEnergyResponseModel meteringData = identifiableMeteredData.payload().data();
        ValidatedHistoricalDataDataNeed dataNeed = ((ValidatedHistoricalDataDataNeed) dataNeedsService.
                getById(identifiableMeteredData.permissionRequest().dataNeedId()));

        if (meteringData == null) {
            return List.of();
        }

        ZonedDateTime fetchTime = meteringData.fetchTime();
        EnergyType type = dataNeed.energyType();
        return getVHD(meteringData, fetchTime, type);
    }

    private List<ValidatedHistoricalDataEnvelope> getVHD(
            GetEnergyResponseModel responseModel,
            @Nullable ZonedDateTime fetchTime,
            EnergyType energyType
    ) {
        var meteringData = responseModel.getMeterFor(energyType);
        if (meteringData == null) {
            return List.of();
        }

        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();

        for (MeterResponseModel meterResponse : meteringData) {
            int seqNumber = meterResponse.seqNumber();
            ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(
                    meterResponse.getByGranularity(granularity)
            );
            List<TimeSeriesComplexType> timeSeries = getTimeSeries(
                    meterResponse.getByGranularity(granularity),
                    commodityKind(energyType),
                    getFlowDirection(meterResponse.hasOfftake(granularity), meterResponse.hasInjection(granularity)),
                    meterResponse.meterID()
            );

            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime,
                                                                             seqNumber,
                                                                             periodIntervalTime,
                                                                             timeSeries);
            vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
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

    private ValidatedHistoricalDataMarketDocumentComplexType createVHD(
            @Nullable ZonedDateTime fetchTime,
            int seqNumber,
            ESMPDateTimeIntervalComplexType periodIntervalTime,
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
                                .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                .withValue("Fluvius")
                )
                .withReceiverMarketParticipantMRID(
                        new PartyIDStringComplexType()
                                .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                .withValue(fluviusConfig.clientId())
                )
                .withDescription(String.valueOf(seqNumber))
                .withPeriodTimeInterval(periodIntervalTime)
                .withCreatedDateTime(new EsmpDateTime(fetchTime).toString())
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(timeSeriesList)
                );
    }

    private List<TimeSeriesComplexType> createTimeSeriesList(
            Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap,
            CommodityKind type,
            DirectionTypeList flowDirection,
            @Nullable String meterId
    ) {
        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
        for (Map.Entry<String, SeriesPeriodComplexType> entry : unitSeriesPeriodMap.entrySet()) {
            timeSeriesList.add(
                    new TimeSeriesComplexType()
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(getBusinessType(flowDirection))
                            .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(type)
                            .withFlowDirectionDirection(flowDirection)
                            .withEnergyMeasurementUnitName(getUnitCIM(entry.getKey()))
                            .withMarketEvaluationPointMRID(
                                    new MeasurementPointIDStringComplexType()
                                            .withCodingScheme(CodingSchemeTypeList.BELGIUM_NATIONAL_CODING_SCHEME)
                                            .withValue(meterId)
                            )
                            .withReasonList(new TimeSeriesComplexType.ReasonList()
                                                    .withReasons(
                                                            new ReasonComplexType()
                                                                    .withCode(
                                                                            ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED
                                                                    )
                                                    )
                            )
                            .withSeriesPeriodList(
                                    new TimeSeriesComplexType.SeriesPeriodList()
                                            .withSeriesPeriods(entry.getValue())
                            )
            );
        }

        return timeSeriesList;
    }

    private UnitOfMeasureTypeList getUnitCIM(String unit) {
        return unit.equalsIgnoreCase("m3") ? UnitOfMeasureTypeList.CUBIC_METRE
                : UnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private List<TimeSeriesComplexType> getTimeSeries(
            @Nullable List<? extends EnergyItemResponseModel<?>> energyItems,
            CommodityKind commodityKind,
            DirectionTypeList direction,
            @Nullable String meterId
    ) {
        if (energyItems == null) {
            return List.of();
        }

        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();

        for (EnergyItemResponseModel<?> item : energyItems) {
            Map<String, List<PointComplexType>> unitPointsMap = getPoints(item);
            Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(
                    item.timestampStart(),
                    item.timestampEnd(),
                    unitPointsMap
            );
            timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, commodityKind, direction, meterId));
        }

        return timeSeriesList;
    }

    private Map<String, SeriesPeriodComplexType> getSeriesPeriods(
            ZonedDateTime timestampStart,
            ZonedDateTime timestampEnd,
            Map<String, List<PointComplexType>> unitPointsMap
    ) {
        Map<String, SeriesPeriodComplexType> seriesPeriodMap = new HashMap<>();
        for (Map.Entry<String, List<PointComplexType>> entry : unitPointsMap.entrySet()) {
            seriesPeriodMap.put(entry.getKey(), createSeriesPeriod(timestampStart, timestampEnd, entry.getValue()));
        }

        return seriesPeriodMap;
    }

    private Map<String, List<PointComplexType>> getPoints(EnergyItemResponseModel<? extends MeasurementResponseModel> response) {
        List<? extends MeasurementResponseModel> measurements = response.measurement();
        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();

        for (int i = 0; i < measurements.size(); i++) {
            MeasurementResponseModel measurement = measurements.get(i);
            String unit = measurement.unit();
            Double quantity = measurement.value();
            QualityTypeList quality = getQualityType(measurement.validationStates());

            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(i + 1, quantity, quality));
        }

        return pointsMap;
    }

    private PointComplexType createPoint(int position, Double quantity, QualityTypeList quality) {
        return new PointComplexType()
                .withPosition(Integer.toString(position))
                .withEnergyQuantityQuantity(quality == null ? null : BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(quality);
    }

    private QualityTypeList getQualityType(ValidationState... validationStates) {
        for (ValidationState state : validationStates) {
            if (state == ValidationState.EST) {
                return QualityTypeList.ESTIMATED;
            }
        }
        return QualityTypeList.AS_PROVIDED;
    }

    private SeriesPeriodComplexType createSeriesPeriod(
            ZonedDateTime timestampStart,
            ZonedDateTime timestampEnd,
            List<PointComplexType> points
    ) {
        EsmpTimeInterval interval = new EsmpTimeInterval(timestampStart, timestampEnd);
        return new SeriesPeriodComplexType()
                .withResolution(granularity.toString())
                .withTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));
    }

    private DirectionTypeList getFlowDirection(boolean hasOfftake, boolean hasInjection) {
        if (hasOfftake) {
            return hasInjection ? DirectionTypeList.UP_AND_DOWN : DirectionTypeList.DOWN;
        } else {
            return DirectionTypeList.UP;
        }
    }

    @Nullable
    private BusinessTypeList getBusinessType(DirectionTypeList direction) {
        return switch (direction) {
            case UP -> BusinessTypeList.PRODUCTION;
            case DOWN -> BusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> BusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            default -> null;
        };
    }

    @SuppressWarnings({"NullAway", "javabugs:S2259"})
    // False positive for items, since it is checked in a separate method
    private ESMPDateTimeIntervalComplexType getPeriodTimeInterval(@Nullable List<? extends EnergyItemResponseModel<?>> items) {
        ESMPDateTimeIntervalComplexType periodTimeInterval = new ESMPDateTimeIntervalComplexType();

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
