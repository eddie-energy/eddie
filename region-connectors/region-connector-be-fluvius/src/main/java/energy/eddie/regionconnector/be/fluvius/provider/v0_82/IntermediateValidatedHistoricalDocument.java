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
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

class IntermediateValidatedHistoricalDocument {

    private static final String EST = "EST";
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
        return switch (type) {
            case ELECTRICITY -> getVHD(
                    meteringData.electricityMeters(),
                    fetchTime,
                    r -> r.seqNumber() == null ? null : String.valueOf(r.seqNumber()),
                    this::getPeriodTimeIntervalFromMeterResponse,
                    this::getElectricityTimeSeries
            );
            case NATURAL_GAS -> getVHD(
                    meteringData.gasMeters(),
                    fetchTime,
                    r -> r.seqNumber() == null ? null : String.valueOf(r.seqNumber()),
                    this::getPeriodTimeIntervalFromMeterResponse,
                    this::getGasTimeSeries
            );
            default -> throw new IllegalStateException("Unexpected energy type: " + type);
        };
    }

    private <T> List<ValidatedHistoricalDataEnvelope> getVHD(
            @Nullable List<T> meteringData,
            @Nullable ZonedDateTime fetchTime,
            Function<T, String> getSeqNumber,
            Function<T, ESMPDateTimeIntervalComplexType> periodIntervalProvider,
            Function<T, List<TimeSeriesComplexType>> timeSeriesProvider
    ) {
        if (meteringData == null) {
            return List.of();
        }

        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();

        for (T meterResponse : meteringData) {
            String seqNumber = getSeqNumber.apply(meterResponse);
            ESMPDateTimeIntervalComplexType periodIntervalTime = periodIntervalProvider.apply(meterResponse);
            List<TimeSeriesComplexType> timeSeries = timeSeriesProvider.apply(meterResponse);

            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime,
                                                                             seqNumber,
                                                                             periodIntervalTime,
                                                                             timeSeries);
            vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }

        return vhds;
    }

    private ValidatedHistoricalDataMarketDocumentComplexType createVHD(
            @Nullable ZonedDateTime fetchTime,
            String seqNumber,
            ESMPDateTimeIntervalComplexType periodIntervalTime,
            List<TimeSeriesComplexType> timeSeriesList
    ) {
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
                .withDescription(seqNumber)
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

    private List<TimeSeriesComplexType> getElectricityTimeSeries(@Nullable ElectricityMeterResponseModel electricityMeterResponse) {
        if (electricityMeterResponse == null) {
            return List.of();
        }
        boolean offtakeDayValue = checkEnergyMeasurementValue(electricityMeterResponse.dailyEnergy(),
                                                              EDailyEnergyItemResponseModel::measurement, m ->
                                                                      checkDailyMeasurementValues(m.offtakeDayValue(),
                                                                                                  m.offtakeNightValue()));
        boolean injectionDayValue = checkEnergyMeasurementValue(electricityMeterResponse.dailyEnergy(),
                                                                EDailyEnergyItemResponseModel::measurement, m ->
                                                                        checkDailyMeasurementValues(m.injectionDayValue(),
                                                                                                    m.injectionNightValue()));
        boolean offtakeValue = checkEnergyMeasurementValue(electricityMeterResponse.quarterHourlyEnergy(),
                                                           EQuarterHourlyEnergyItemResponseModel::measurement,
                                                           m -> m.offtakeValue() != null);
        boolean injectionValue = checkEnergyMeasurementValue(electricityMeterResponse.quarterHourlyEnergy(),
                                                             EQuarterHourlyEnergyItemResponseModel::measurement,
                                                             m -> m.injectionValue() != null);
        DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue,
                                                           injectionDayValue,
                                                           offtakeValue,
                                                           injectionValue);
        boolean offtake = Objects.equals(flowDirection, DirectionTypeList.UP);

        return granularity.equals(Granularity.P1D) ?
                getTimeSeries(electricityMeterResponse.dailyEnergy(),
                              EDailyEnergyItemResponseModel::timestampStart,
                              EDailyEnergyItemResponseModel::timestampEnd,
                              item -> getPoints(item, EDailyEnergyItemResponseModel::measurement,
                                                EMeasurementItemResponseModel::unit,
                                                m -> getEDailyQuantity(m, offtake),
                                                m -> getEDailyQuality(m, offtake)
                              ),
                              CommodityKind.ELECTRICITYPRIMARYMETERED,
                              flowDirection,
                              electricityMeterResponse.meterID()
                ) : getTimeSeries(electricityMeterResponse.quarterHourlyEnergy(),
                                  EQuarterHourlyEnergyItemResponseModel::timestampStart,
                                  EQuarterHourlyEnergyItemResponseModel::timestampEnd,
                                  item -> getPoints(item,
                                                    EQuarterHourlyEnergyItemResponseModel::measurement,
                                                    EMeasurementDetailItemResponseModel::unit,
                                                    m -> getEQuarterHourlyQuantity(m, offtake),
                                                    m -> getEQuarterHourlyQuality(m, offtake)
                                  ),
                                  CommodityKind.ELECTRICITYPRIMARYMETERED,
                                  flowDirection,
                                  electricityMeterResponse.meterID());
    }

    private List<TimeSeriesComplexType> getGasTimeSeries(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D) ?
                getTimeSeries(gasMeterResponse.dailyEnergy(),
                              GDailyEnergyItemResponseModel::timestampStart,
                              GDailyEnergyItemResponseModel::timestampEnd,
                              item -> getPoints(item, GDailyEnergyItemResponseModel::measurement,
                                                GMeasurementItemResponseModel::unit,
                                                m -> Optional.ofNullable(m.offtakeValue()).orElse(0.0),
                                                m -> getQualityType(m.offtakeValidationState())
                              ),
                              CommodityKind.NATURALGAS,
                              DirectionTypeList.DOWN,
                              gasMeterResponse.meterID()
                ) : getTimeSeries(gasMeterResponse.hourlyEnergy(),
                                  GHourlyEnergyItemResponseModel::timestampStart,
                                  GHourlyEnergyItemResponseModel::timestampEnd,
                                  item -> getPoints(item, GHourlyEnergyItemResponseModel::measurement,
                                                    GMeasurementDetailItemResponseModel::unit,
                                                    m -> Optional.ofNullable(m.offtakeValue()).orElse(0.0),
                                                    m -> getQualityType(m.offtakeValidationState())
                                  ),
                                  CommodityKind.NATURALGAS,
                                  DirectionTypeList.DOWN,
                                  gasMeterResponse.meterID());
    }

    private UnitOfMeasureTypeList getUnitCIM(String unit) {
        return unit.equalsIgnoreCase("m3") ? UnitOfMeasureTypeList.CUBIC_METRE
                : UnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private <T> List<TimeSeriesComplexType> getTimeSeries(
            @Nullable List<T> energyItems,
            Function<T, ZonedDateTime> getTimestampStart,
            Function<T, ZonedDateTime> getTimestampEnd,
            Function<T, Map<String, List<PointComplexType>>> pointMapper,
            CommodityKind commodityKind,
            DirectionTypeList direction,
            @Nullable String meterId
    ) {
        if (energyItems == null) {
            return List.of();
        }

        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();

        for (T item : energyItems) {
            Map<String, List<PointComplexType>> unitPointsMap = pointMapper.apply(item);
            Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(
                    getTimestampStart.apply(item),
                    getTimestampEnd.apply(item),
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

    private <R, M> Map<String, List<PointComplexType>> getPoints(
            R response,
            Function<R, List<M>> measurementListExtractor,
            Function<M, String> unitExtractor,
            ToDoubleFunction<M> quantityExtractor,
            Function<M, QualityTypeList> qualityExtractor
    ) {
        List<M> measurements = measurementListExtractor.apply(response);
        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();

        for (int i = 0; i < measurements.size(); i++) {
            M measurement = measurements.get(i);
            String unit = unitExtractor.apply(measurement);
            Double quantity = quantityExtractor.applyAsDouble(measurement);
            QualityTypeList quality = qualityExtractor.apply(measurement);

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

    private double getEDailyQuantity(EMeasurementItemResponseModel m, boolean offtake) {
        return offtake ? sumNullSafe(m.offtakeDayValue(), m.offtakeNightValue())
                : sumNullSafe(m.injectionDayValue(), m.injectionNightValue());
    }

    private double getEQuarterHourlyQuantity(EMeasurementDetailItemResponseModel m, boolean offtake) {
        return offtake ? Optional.ofNullable(m.offtakeValue()).orElse(0.0)
                : Optional.ofNullable(m.injectionValue()).orElse(0.0);
    }

    private Double sumNullSafe(@Nullable Double first, @Nullable Double second) {
        return Optional.ofNullable(first).orElse(0.0) + Optional.ofNullable(second).orElse(0.0);
    }

    private QualityTypeList getEDailyQuality(EMeasurementItemResponseModel m, boolean offtake) {
        return offtake ? getQualityType(m.offtakeDayValidationState(), m.offtakeNightValidationState())
                : getQualityType(m.injectionDayValidationState(), m.injectionNightValidationState());
    }

    private QualityTypeList getEQuarterHourlyQuality(EMeasurementDetailItemResponseModel m, boolean offtake) {
        return getQualityType(offtake ? m.offtakeValidationState() : m.injectionValidationState());
    }

    private QualityTypeList getQualityType(@Nullable String... validationStates) {
        if(validationStates == null) {
            return QualityTypeList.AS_PROVIDED;
        }
        for (String state : validationStates) {
            if (Objects.equals(state, EST)) {
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

    private DirectionTypeList getFlowDirection(
            boolean offtakeDayValue, boolean injectionDayValue,
            boolean offtakeValue, boolean injectionValue
    ) {
        if (offtakeValue || offtakeDayValue) {
            return injectionValue || injectionDayValue ? DirectionTypeList.UP_AND_DOWN : DirectionTypeList.DOWN;
        } else {
            return DirectionTypeList.UP;
        }
    }

    private <T, U> boolean checkEnergyMeasurementValue(
            @Nullable List<T> response,
            Function<T, List<U>> measurementExtractor,
            Predicate<U> valueChecker
    ) {

        if (response == null) {
            return false;
        }

        for (T item : response) {
            List<U> measurements = measurementExtractor.apply(item);
            if (measurements == null) {
                continue;
            }

            for (U measurement : measurements) {
                if (valueChecker.test(measurement)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkDailyMeasurementValues(@Nullable Double dayVal, @Nullable Double nightVal) {
        return dayVal != null && nightVal != null;
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

    private ESMPDateTimeIntervalComplexType getPeriodTimeIntervalFromMeterResponse(
            ElectricityMeterResponseModel electricityMeterResponse
    ) {
        return granularity.equals(Granularity.P1D) ? getPeriodTimeInterval(electricityMeterResponse.dailyEnergy(),
                                                                           EDailyEnergyItemResponseModel::timestampStart,
                                                                           EDailyEnergyItemResponseModel::timestampEnd)
                : getPeriodTimeInterval(electricityMeterResponse.quarterHourlyEnergy(),
                                        EQuarterHourlyEnergyItemResponseModel::timestampStart,
                                        EQuarterHourlyEnergyItemResponseModel::timestampEnd);
    }

    private ESMPDateTimeIntervalComplexType getPeriodTimeIntervalFromMeterResponse(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D) ? getPeriodTimeInterval(gasMeterResponse.dailyEnergy(),
                                                                           GDailyEnergyItemResponseModel::timestampStart,
                                                                           GDailyEnergyItemResponseModel::timestampEnd)
                : getPeriodTimeInterval(gasMeterResponse.hourlyEnergy(),
                                        GHourlyEnergyItemResponseModel::timestampStart,
                                        GHourlyEnergyItemResponseModel::timestampEnd);
    }

    @SuppressWarnings("NullAway") // False positive for items, since it is checked in a separate method
    private <T> ESMPDateTimeIntervalComplexType getPeriodTimeInterval(
            @Nullable List<T> items,
            Function<T, ZonedDateTime> startExtractor,
            Function<T, ZonedDateTime> endExtractor
    ) {
        ESMPDateTimeIntervalComplexType periodTimeInterval = new ESMPDateTimeIntervalComplexType();

        if (isNullOrEmpty(items)) {
            return periodTimeInterval;
        }

        ZonedDateTime earliestTimestampStart = startExtractor.apply(items.getFirst());
        ZonedDateTime latestTimestampEnd = endExtractor.apply(items.getFirst());

        for (T item : items) {
            earliestTimestampStart = getEarliestTimestamp(earliestTimestampStart, startExtractor.apply(item));
            latestTimestampEnd = getLatestTimestamp(latestTimestampEnd, endExtractor.apply(item));
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

    @Nullable
    private static ZonedDateTime getLatestTimestamp(@Nullable ZonedDateTime current, @Nullable ZonedDateTime other) {
        return other != null && other.isAfter(current) ? other : current;
    }

    @Nullable
    private static ZonedDateTime getEarliestTimestamp(
            @Nullable ZonedDateTime current,
            @Nullable ZonedDateTime other
    ) {
        return other != null && other.isBefore(current) ? other : current;
    }
}
