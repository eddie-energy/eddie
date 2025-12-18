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

    public List<VHDEnvelope> toVHD() {
        GetEnergyResponseModel meteringData = identifiableMeteredData.payload().data();
        if (meteringData == null) {
            return List.of();
        }
        ValidatedHistoricalDataDataNeed dataNeed = ((ValidatedHistoricalDataDataNeed) dataNeedsService.
                getById(identifiableMeteredData.permissionRequest().dataNeedId()));

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

    private <T> List<VHDEnvelope> getVHD(
            @Nullable List<T> meteringData,
            @Nullable ZonedDateTime fetchTime,
            Function<T, String> getSeqNumber,
            Function<T, ESMPDateTimeInterval> periodIntervalProvider,
            Function<T, List<TimeSeries>> timeSeriesProvider
    ) {
        if (meteringData == null) {
            return List.of();
        }

        List<VHDEnvelope> vhds = new ArrayList<>();
        for (T meterResponse : meteringData) {
            String seqNumber = getSeqNumber.apply(meterResponse);
            ESMPDateTimeInterval periodIntervalTime = periodIntervalProvider.apply(meterResponse);
            List<TimeSeries> timeSeries = timeSeriesProvider.apply(meterResponse);

            VHDMarketDocument vhd = createVHD(fetchTime, seqNumber, periodIntervalTime, timeSeries);
            vhds.add(new VhdEnvelopeWrapper(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }

        return vhds;
    }

    private VHDMarketDocument createVHD(
            @Nullable ZonedDateTime fetchTime,
            String seqNumber,
            ESMPDateTimeInterval periodIntervalTime,
            List<TimeSeries> timeSeriesList
    ) {
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
                .withDescription(seqNumber)
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

    private List<TimeSeries> getElectricityTimeSeries(ElectricityMeterResponseModel electricityMeterResponse) {
        boolean offtakeDayValue = checkEnergyMeasurementValue(
                electricityMeterResponse.dailyEnergy(),
                EDailyEnergyItemResponseModel::measurement,
                m -> checkDailyMeasurementValues(m.offtakeDayValue(), m.offtakeNightValue())
        );
        boolean injectionDayValue = checkEnergyMeasurementValue(
                electricityMeterResponse.dailyEnergy(),
                EDailyEnergyItemResponseModel::measurement,
                m -> checkDailyMeasurementValues(m.injectionDayValue(), m.injectionNightValue())
        );
        boolean offtakeValue = checkEnergyMeasurementValue(
                electricityMeterResponse.quarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::measurement,
                m -> m.offtakeValue() != null
        );
        boolean injectionValue = checkEnergyMeasurementValue(
                electricityMeterResponse.quarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::measurement,
                m -> m.injectionValue() != null
        );
        StandardDirectionTypeList flowDirection = getFlowDirection(
                offtakeDayValue,
                injectionDayValue,
                offtakeValue,
                injectionValue
        );

        return granularity.equals(Granularity.P1D)
                ? getTimeSeries(
                electricityMeterResponse.dailyEnergy(),
                EDailyEnergyItemResponseModel::timestampStart,
                EDailyEnergyItemResponseModel::timestampEnd,
                item -> getPoints(
                        item,
                        EDailyEnergyItemResponseModel::measurement,
                        EMeasurementItemResponseModel::unit,
                        m -> getEDailyQuantity(m, flowDirection),
                        m -> getEDailyQuality(m, flowDirection)
                ),
                CommodityKind.ELECTRICITYPRIMARYMETERED,
                flowDirection,
                electricityMeterResponse.meterID()
        )
                : getTimeSeries(
                electricityMeterResponse.quarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::timestampStart,
                EQuarterHourlyEnergyItemResponseModel::timestampEnd,
                item -> getPoints(
                        item,
                        EQuarterHourlyEnergyItemResponseModel::measurement,
                        EMeasurementDetailItemResponseModel::unit,
                        m -> getEQuarterHourlyQuantity(m, flowDirection),
                        m -> getEQuarterHourlyQuality(m, flowDirection)
                ),
                CommodityKind.ELECTRICITYPRIMARYMETERED,
                flowDirection,
                electricityMeterResponse.meterID()
        );
    }

    private List<TimeSeries> getGasTimeSeries(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D)
                ? getTimeSeries(gasMeterResponse.dailyEnergy(),
                                GDailyEnergyItemResponseModel::timestampStart,
                                GDailyEnergyItemResponseModel::timestampEnd,
                                item -> getPoints(
                                        item,
                                        GDailyEnergyItemResponseModel::measurement,
                                        GMeasurementItemResponseModel::unit,
                                        m -> Optional.ofNullable(m.offtakeValue()).orElse(0.0),
                                        m -> getQualityType(m.offtakeValidationState())
                                ),
                                CommodityKind.NATURALGAS,
                                StandardDirectionTypeList.DOWN,
                                gasMeterResponse.meterID()
        )
                : getTimeSeries(
                gasMeterResponse.hourlyEnergy(),
                GHourlyEnergyItemResponseModel::timestampStart,
                GHourlyEnergyItemResponseModel::timestampEnd,
                item -> getPoints(
                        item, GHourlyEnergyItemResponseModel::measurement,
                        GMeasurementDetailItemResponseModel::unit,
                        m -> Optional.ofNullable(m.offtakeValue()).orElse(0.0),
                        m -> getQualityType(m.offtakeValidationState()
                        )
                ),
                CommodityKind.NATURALGAS,
                StandardDirectionTypeList.DOWN,
                gasMeterResponse.meterID()
        );
    }

    private StandardUnitOfMeasureTypeList getUnitCIM(String unit) {
        return unit.equalsIgnoreCase("m3")
                ? StandardUnitOfMeasureTypeList.CUBIC_METRE
                : StandardUnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private <T> List<TimeSeries> getTimeSeries(
            @Nullable List<T> energyItems,
            Function<T, ZonedDateTime> getTimestampStart,
            Function<T, ZonedDateTime> getTimestampEnd,
            Function<T, Map<String, List<Point>>> pointMapper,
            CommodityKind commodityKind,
            StandardDirectionTypeList direction,
            @Nullable String meterId
    ) {
        if (energyItems == null) {
            return List.of();
        }

        List<TimeSeries> timeSeriesList = new ArrayList<>();

        for (T item : energyItems) {
            Map<String, List<Point>> unitPointsMap = pointMapper.apply(item);
            Map<String, SeriesPeriod> unitSeriesPeriodMap = getSeriesPeriods(
                    getTimestampStart.apply(item),
                    getTimestampEnd.apply(item),
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

    private <R, M> Map<String, List<Point>> getPoints(
            R response,
            Function<R, List<M>> measurementListExtractor,
            Function<M, String> unitExtractor,
            ToDoubleFunction<M> quantityExtractor,
            Function<M, StandardQualityTypeList> qualityExtractor
    ) {
        List<M> measurements = measurementListExtractor.apply(response);
        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<Point>> pointsMap = new HashMap<>();

        for (int i = 0; i < measurements.size(); i++) {
            M measurement = measurements.get(i);
            String unit = unitExtractor.apply(measurement);
            Double quantity = quantityExtractor.applyAsDouble(measurement);
            StandardQualityTypeList quality = qualityExtractor.apply(measurement);

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

    private double getEDailyQuantity(EMeasurementItemResponseModel m, StandardDirectionTypeList direction) {
        return switch (direction) {
            case UP -> sumNullSafe(m.injectionDayValue(), m.injectionNightValue());
            case UP_AND_DOWN -> sumNullSafe(m.offtakeDayValue(), m.offtakeNightValue())
                                - sumNullSafe(m.injectionDayValue(), m.injectionNightValue());
            case null, default -> sumNullSafe(m.offtakeDayValue(), m.offtakeNightValue());
        };
    }

    private double getEQuarterHourlyQuantity(
            EMeasurementDetailItemResponseModel m,
            StandardDirectionTypeList direction
    ) {
        var offtake = Optional.ofNullable(m.offtakeValue()).orElse(0.0);
        var injection = Optional.ofNullable(m.injectionValue()).orElse(0.0);
        return switch (direction) {
            case UP -> injection;
            case UP_AND_DOWN -> offtake - injection;
            case null, default -> offtake;
        };
    }

    private Double sumNullSafe(@Nullable Double first, @Nullable Double second) {
        return Optional.ofNullable(first).orElse(0.0) + Optional.ofNullable(second).orElse(0.0);
    }

    private StandardQualityTypeList getEDailyQuality(
            EMeasurementItemResponseModel m,
            StandardDirectionTypeList direction
    ) {
        return switch (direction) {
            case UP -> getQualityType(m.injectionDayValidationState(), m.injectionNightValidationState());
            case UP_AND_DOWN -> getQualityType(m.injectionDayValidationState(),
                                               m.injectionNightValidationState(),
                                               m.offtakeDayValidationState(),
                                               m.offtakeNightValidationState());
            case null, default -> getQualityType(m.offtakeDayValidationState(), m.offtakeNightValidationState());
        };
    }

    private StandardQualityTypeList getEQuarterHourlyQuality(
            EMeasurementDetailItemResponseModel m,
            StandardDirectionTypeList direction
    ) {
        return switch (direction) {
            case DOWN -> getQualityType(m.offtakeValidationState());
            case UP_AND_DOWN -> getQualityType(m.offtakeValidationState(), m.injectionValidationState());
            case null, default -> getQualityType(m.injectionValidationState());
        };
    }

    private StandardQualityTypeList getQualityType(@Nullable String... validationStates) {
        if(validationStates == null) {
            return StandardQualityTypeList.AS_PROVIDED;
        }
        for (String state : validationStates) {
            if (Objects.equals(state, EST)) {
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

    private StandardDirectionTypeList getFlowDirection(
            boolean offtakeDayValue,
            boolean injectionDayValue,
            boolean offtakeValue,
            boolean injectionValue
    ) {
        boolean isOfftake = offtakeValue || offtakeDayValue;
        boolean isInjection = injectionDayValue || injectionValue;
        if (isOfftake) {
            return isInjection ? StandardDirectionTypeList.UP_AND_DOWN : StandardDirectionTypeList.DOWN;
        } else {
            return StandardDirectionTypeList.UP;
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
    private StandardBusinessTypeList getBusinessType(StandardDirectionTypeList direction) {
        return switch (direction) {
            case UP -> StandardBusinessTypeList.PRODUCTION;
            case DOWN -> StandardBusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> StandardBusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            default -> null;
        };
    }

    private ESMPDateTimeInterval getPeriodTimeIntervalFromMeterResponse(
            ElectricityMeterResponseModel electricityMeterResponse
    ) {
        return granularity.equals(Granularity.P1D)
                ? getPeriodTimeInterval(electricityMeterResponse.dailyEnergy(),
                                        EDailyEnergyItemResponseModel::timestampStart,
                                        EDailyEnergyItemResponseModel::timestampEnd)
                : getPeriodTimeInterval(electricityMeterResponse.quarterHourlyEnergy(),
                                        EQuarterHourlyEnergyItemResponseModel::timestampStart,
                                        EQuarterHourlyEnergyItemResponseModel::timestampEnd);
    }

    private ESMPDateTimeInterval getPeriodTimeIntervalFromMeterResponse(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D)
                ? getPeriodTimeInterval(gasMeterResponse.dailyEnergy(),
                                        GDailyEnergyItemResponseModel::timestampStart,
                                        GDailyEnergyItemResponseModel::timestampEnd)
                : getPeriodTimeInterval(gasMeterResponse.hourlyEnergy(),
                                        GHourlyEnergyItemResponseModel::timestampStart,
                                        GHourlyEnergyItemResponseModel::timestampEnd);
    }


    @SuppressWarnings("NullAway") // False positive for items, since it is checked in a separate method
    private <T> ESMPDateTimeInterval getPeriodTimeInterval(
            @Nullable List<T> items,
            Function<T, ZonedDateTime> startExtractor,
            Function<T, ZonedDateTime> endExtractor
    ) {
        ESMPDateTimeInterval periodTimeInterval = new ESMPDateTimeInterval();
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
