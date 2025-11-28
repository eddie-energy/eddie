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
import java.time.OffsetDateTime;
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
        GetEnergyResponseModel meteringData = identifiableMeteredData.payload().getData();
        if (meteringData == null) {
            return List.of();
        }
        ValidatedHistoricalDataDataNeed dataNeed = ((ValidatedHistoricalDataDataNeed) dataNeedsService.
                getById(identifiableMeteredData.permissionRequest().dataNeedId()));

        OffsetDateTime fetchTime = meteringData.getFetchTime();
        EnergyType type = dataNeed.energyType();
        return switch (type) {
            case ELECTRICITY -> getVHD(
                    meteringData.getElectricityMeters(),
                    fetchTime,
                    r -> r.getSeqNumber() == null ? null : String.valueOf(r.getSeqNumber()),
                    this::getPeriodTimeIntervalFromMeterResponse,
                    this::getElectricityTimeSeries
            );
            case NATURAL_GAS -> getVHD(
                    meteringData.getGasMeters(),
                    fetchTime,
                    r -> r.getSeqNumber() == null ? null : String.valueOf(r.getSeqNumber()),
                    this::getPeriodTimeIntervalFromMeterResponse,
                    this::getGasTimeSeries
            );
            default -> throw new IllegalStateException("Unexpected energy type: " + type);
        };
    }

    private <T> List<VHDEnvelope> getVHD(
            List<T> meteringData,
            OffsetDateTime fetchTime,
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
            OffsetDateTime fetchTime,
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
                .withCreatedDateTime(fetchTime.toZonedDateTime())
                .withTimeSeries(timeSeriesList);
    }

    private List<TimeSeries> createTimeSeriesList(
            Map<String, SeriesPeriod> unitSeriesPeriodMap,
            CommodityKind type,
            StandardDirectionTypeList flowDirection,
            String meterId
    ) {
        List<TimeSeries> timeSeriesList = new ArrayList<>();
        var standardBusinessType = getBusinessType(flowDirection);
        var businessType = standardBusinessType == null ? null : standardBusinessType.value();
        var metaData = identifiableMeteredData.payload().getMetaData();
        var version = metaData == null ? "1" : metaData.getVersion();
        for (Map.Entry<String, SeriesPeriod> entry : unitSeriesPeriodMap.entrySet()) {
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
                electricityMeterResponse.getDailyEnergy(),
                EDailyEnergyItemResponseModel::getMeasurement,
                m -> checkDailyMeasurementValues(m.getOfftakeDayValue(), m.getOfftakeNightValue())
        );
        boolean injectionDayValue = checkEnergyMeasurementValue(
                electricityMeterResponse.getDailyEnergy(),
                EDailyEnergyItemResponseModel::getMeasurement,
                m -> checkDailyMeasurementValues(m.getInjectionDayValue(), m.getInjectionNightValue())
        );
        boolean offtakeValue = checkEnergyMeasurementValue(
                electricityMeterResponse.getQuarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::getMeasurement,
                m -> m.getOfftakeValue() != null
        );
        boolean injectionValue = checkEnergyMeasurementValue(
                electricityMeterResponse.getQuarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::getMeasurement,
                m -> m.getInjectionValue() != null
        );
        StandardDirectionTypeList flowDirection = getFlowDirection(
                offtakeDayValue,
                injectionDayValue,
                offtakeValue,
                injectionValue
        );

        return granularity.equals(Granularity.P1D)
                ? getTimeSeries(
                electricityMeterResponse.getDailyEnergy(),
                EDailyEnergyItemResponseModel::getTimestampStart,
                EDailyEnergyItemResponseModel::getTimestampEnd,
                item -> getPoints(
                        item,
                        EDailyEnergyItemResponseModel::getMeasurement,
                        EMeasurementItemResponseModel::getUnit,
                        m -> getEDailyQuantity(m, flowDirection),
                        m -> getEDailyQuality(m, flowDirection)
                ),
                CommodityKind.ELECTRICITYPRIMARYMETERED,
                flowDirection,
                electricityMeterResponse.getMeterID()
        )
                : getTimeSeries(
                electricityMeterResponse.getQuarterHourlyEnergy(),
                EQuarterHourlyEnergyItemResponseModel::getTimestampStart,
                EQuarterHourlyEnergyItemResponseModel::getTimestampEnd,
                item -> getPoints(
                        item,
                        EQuarterHourlyEnergyItemResponseModel::getMeasurement,
                        EMeasurementDetailItemResponseModel::getUnit,
                        m -> getEQuarterHourlyQuantity(m, flowDirection),
                        m -> getEQuarterHourlyQuality(m, flowDirection)
                ),
                CommodityKind.ELECTRICITYPRIMARYMETERED,
                flowDirection,
                electricityMeterResponse.getMeterID()
        );
    }

    private List<TimeSeries> getGasTimeSeries(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D)
                ? getTimeSeries(gasMeterResponse.getDailyEnergy(),
                                GDailyEnergyItemResponseModel::getTimestampStart,
                                GDailyEnergyItemResponseModel::getTimestampEnd,
                                item -> getPoints(
                                        item,
                                        GDailyEnergyItemResponseModel::getMeasurement,
                                        GMeasurementItemResponseModel::getUnit,
                                        m -> Optional.ofNullable(m.getOfftakeValue()).orElse(0.0),
                                        m -> getQualityType(m.getOfftakeValidationState())
                                ),
                                CommodityKind.NATURALGAS,
                                StandardDirectionTypeList.DOWN,
                                gasMeterResponse.getMeterID()
        )
                : getTimeSeries(
                gasMeterResponse.getHourlyEnergy(),
                GHourlyEnergyItemResponseModel::getTimestampStart,
                GHourlyEnergyItemResponseModel::getTimestampEnd,
                item -> getPoints(
                        item, GHourlyEnergyItemResponseModel::getMeasurement,
                        GMeasurementDetailItemResponseModel::getUnit,
                        m -> Optional.ofNullable(m.getOfftakeValue()).orElse(0.0),
                        m -> getQualityType(m.getOfftakeValidationState()
                        )
                ),
                CommodityKind.NATURALGAS,
                StandardDirectionTypeList.DOWN,
                gasMeterResponse.getMeterID()
        );
    }

    private StandardUnitOfMeasureTypeList getUnitCIM(String unit) {
        return unit.equalsIgnoreCase("m3")
                ? StandardUnitOfMeasureTypeList.CUBIC_METRE
                : StandardUnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private <T> List<TimeSeries> getTimeSeries(
            List<T> energyItems,
            Function<T, OffsetDateTime> getTimestampStart,
            Function<T, OffsetDateTime> getTimestampEnd,
            Function<T, Map<String, List<Point>>> pointMapper,
            CommodityKind commodityKind,
            StandardDirectionTypeList direction,
            String meterId
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
            OffsetDateTime timestampStart,
            OffsetDateTime timestampEnd, Map<String, List<Point>> unitPointsMap
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
            case UP -> sumNullSafe(m.getInjectionDayValue(), m.getInjectionNightValue());
            case UP_AND_DOWN -> sumNullSafe(m.getOfftakeDayValue(), m.getOfftakeNightValue())
                                - sumNullSafe(m.getInjectionDayValue(), m.getInjectionNightValue());
            case null, default -> sumNullSafe(m.getOfftakeDayValue(), m.getOfftakeNightValue());
        };
    }

    private double getEQuarterHourlyQuantity(
            EMeasurementDetailItemResponseModel m,
            StandardDirectionTypeList direction
    ) {
        var offtake = Optional.ofNullable(m.getOfftakeValue()).orElse(0.0);
        var injection = Optional.ofNullable(m.getInjectionValue()).orElse(0.0);
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
            case UP -> getQualityType(m.getInjectionDayValidationState(), m.getInjectionNightValidationState());
            case UP_AND_DOWN -> getQualityType(m.getInjectionDayValidationState(),
                                               m.getInjectionNightValidationState(),
                                               m.getOfftakeDayValidationState(),
                                               m.getOfftakeNightValidationState());
            case null, default -> getQualityType(m.getOfftakeDayValidationState(), m.getOfftakeNightValidationState());
        };
    }

    private StandardQualityTypeList getEQuarterHourlyQuality(
            EMeasurementDetailItemResponseModel m,
            StandardDirectionTypeList direction
    ) {
        return switch (direction) {
            case DOWN -> getQualityType(m.getOfftakeValidationState());
            case UP_AND_DOWN -> getQualityType(m.getOfftakeValidationState(), m.getInjectionValidationState());
            case null, default -> getQualityType(m.getInjectionValidationState());
        };
    }

    private StandardQualityTypeList getQualityType(String... validationStates) {
        for (String state : validationStates) {
            if (Objects.equals(state, EST)) {
                return StandardQualityTypeList.ESTIMATED;
            }
        }

        return StandardQualityTypeList.AS_PROVIDED;
    }

    private SeriesPeriod createSeriesPeriod(
            OffsetDateTime timestampStart,
            OffsetDateTime timestampEnd,
            List<Point> points
    ) {
        EsmpTimeInterval interval = new EsmpTimeInterval(timestampStart.toZonedDateTime(),
                                                         timestampEnd.toZonedDateTime());
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
            List<T> response,
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

    private boolean checkDailyMeasurementValues(Double dayVal, Double nightVal) {
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
                ? getPeriodTimeInterval(electricityMeterResponse.getDailyEnergy(),
                                        EDailyEnergyItemResponseModel::getTimestampStart,
                                        EDailyEnergyItemResponseModel::getTimestampEnd)
                : getPeriodTimeInterval(electricityMeterResponse.getQuarterHourlyEnergy(),
                                        EQuarterHourlyEnergyItemResponseModel::getTimestampStart,
                                        EQuarterHourlyEnergyItemResponseModel::getTimestampEnd);
    }

    private ESMPDateTimeInterval getPeriodTimeIntervalFromMeterResponse(GasMeterResponseModel gasMeterResponse) {
        return granularity.equals(Granularity.P1D)
                ? getPeriodTimeInterval(gasMeterResponse.getDailyEnergy(),
                                        GDailyEnergyItemResponseModel::getTimestampStart,
                                        GDailyEnergyItemResponseModel::getTimestampEnd)
                : getPeriodTimeInterval(gasMeterResponse.getHourlyEnergy(),
                                        GHourlyEnergyItemResponseModel::getTimestampStart,
                                        GHourlyEnergyItemResponseModel::getTimestampEnd);
    }


    private <T> ESMPDateTimeInterval getPeriodTimeInterval(
            List<T> items,
            Function<T, OffsetDateTime> startExtractor, Function<T, OffsetDateTime> endExtractor
    ) {
        ESMPDateTimeInterval periodTimeInterval = new ESMPDateTimeInterval();
        if (isNullOrEmpty(items)) {
            return periodTimeInterval;
        }

        OffsetDateTime earliestTimestampStart = startExtractor.apply(items.getFirst());
        OffsetDateTime latestTimestampEnd = endExtractor.apply(items.getFirst());

        for (T item : items) {
            earliestTimestampStart = getEarliestTimestamp(earliestTimestampStart, startExtractor.apply(item));
            latestTimestampEnd = getLatestTimestamp(latestTimestampEnd, endExtractor.apply(item));
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart.toZonedDateTime(),
                                                         latestTimestampEnd.toZonedDateTime());
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }


    private static boolean isNullOrEmpty(@Nullable List<?> list) {
        return list == null || list.isEmpty();
    }

    @Nullable
    private static OffsetDateTime getLatestTimestamp(@Nullable OffsetDateTime current, @Nullable OffsetDateTime other) {
        return other != null && other.isAfter(current) ? other : current;
    }

    @Nullable
    private static OffsetDateTime getEarliestTimestamp(
            @Nullable OffsetDateTime current,
            @Nullable OffsetDateTime other
    ) {
        return other != null && other.isBefore(current) ? other : current;
    }
}
