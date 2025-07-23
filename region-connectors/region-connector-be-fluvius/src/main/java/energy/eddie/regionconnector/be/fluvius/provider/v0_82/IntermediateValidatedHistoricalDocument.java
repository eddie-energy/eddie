package energy.eddie.regionconnector.be.fluvius.provider.v0_82;

import energy.eddie.api.CommonInformationModelVersions;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.be.fluvius.client.model.*;
import energy.eddie.regionconnector.be.fluvius.config.FluviusOAuthConfiguration;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime;
import energy.eddie.regionconnector.shared.cim.v0_82.EsmpTimeInterval;
import energy.eddie.regionconnector.shared.cim.v0_82.vhd.VhdEnvelope;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

class IntermediateValidatedHistoricalDocument {

    private final FluviusOAuthConfiguration fluviusConfig;
    private final IdentifiableMeteringData identifiableMeteredData;
    private final FluviusPermissionRequest fluviusPermissionRequest;
    private final DataNeedsService dataNeedsService;
    private static final String EST = "EST";

    IntermediateValidatedHistoricalDocument(
            FluviusOAuthConfiguration fluviusConfiguration,
            IdentifiableMeteringData identifiableMeteredData,
            DataNeedsService dataNeedsService
    ) {
        this.fluviusConfig = fluviusConfiguration;
        this.fluviusPermissionRequest = (FluviusPermissionRequest) identifiableMeteredData.permissionRequest();
        this.identifiableMeteredData = identifiableMeteredData;
        this.dataNeedsService = dataNeedsService;
    }

    public List<ValidatedHistoricalDataEnvelope> getVHD() {
        GetEnergyResponseModel meteringData = identifiableMeteredData.payload().getData();
        ValidatedHistoricalDataDataNeed dataNeed = ((ValidatedHistoricalDataDataNeed) dataNeedsService.
                getById(identifiableMeteredData.permissionRequest().dataNeedId()));
        
        if (meteringData == null) {
            return List.of();
        }

        OffsetDateTime fetchTime = meteringData.getFetchTime();
        EnergyType type = dataNeed.energyType();
        return switch (type) {
            case ELECTRICITY -> getElectricityVHD(meteringData.getElectricityMeters(), fetchTime);
            case NATURAL_GAS -> getGasVHD(meteringData.getGasMeters(), fetchTime);
            default -> throw new IllegalStateException("Unexpected energy type: " + type);
        };
    }

    private List<ValidatedHistoricalDataEnvelope> getGasVHD(List<GasMeterResponseModel> gasMeteringData,
            OffsetDateTime fetchTime) {

        if(gasMeteringData == null) {
            return List.of();
        }

        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(GasMeterResponseModel meterResponse : gasMeteringData ) {
            String seqNumber = meterResponse.getSeqNumber() == null ? null : String.valueOf(meterResponse.getSeqNumber());
            Granularity granularity = fluviusPermissionRequest.granularity();
            ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime, seqNumber, periodIntervalTime,
                    getTimeSeries(granularity, meterResponse));
            vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }
        return vhds;
    }

    private List<ValidatedHistoricalDataEnvelope> getElectricityVHD(List<ElectricityMeterResponseModel> electricityMeteringData,
            OffsetDateTime fetchTime) {

        if(electricityMeteringData == null) {
            return List.of();
        }
        
        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(ElectricityMeterResponseModel meterResponse : electricityMeteringData) {
            boolean offtakeDayValue = checkEnergyMeasurementValue(meterResponse.getDailyEnergy(),
                    EDailyEnergyItemResponseModel::getMeasurement, m ->
                    checkDailyMeasurementValues(m.getOfftakeDayValue(), m.getOfftakeNightValue()));
            boolean injectionDayValue = checkEnergyMeasurementValue(meterResponse.getDailyEnergy(),
                    EDailyEnergyItemResponseModel::getMeasurement,m ->
                    checkDailyMeasurementValues(m.getInjectionDayValue(), m.getInjectionNightValue()));
            boolean offtakeValue = checkEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(),
                    EQuarterHourlyEnergyItemResponseModel::getMeasurement, m -> m.getOfftakeValue() != null);
            boolean injectionValue = checkEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(),
                    EQuarterHourlyEnergyItemResponseModel::getMeasurement,
                    m -> m.getInjectionValue() != null);
            DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue, injectionDayValue, offtakeValue, injectionValue);
            String seqNumber = meterResponse.getSeqNumber() == null ? null : String.valueOf(meterResponse.getSeqNumber());
            Granularity granularity = fluviusPermissionRequest.granularity();
            ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
            boolean offtake = Objects.equals(flowDirection, DirectionTypeList.UP);
            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime, seqNumber, periodIntervalTime,
                    getTimeSeries(granularity, meterResponse, flowDirection, offtake));
            vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }
        return vhds;
    }

    private ValidatedHistoricalDataMarketDocumentComplexType createVHD(OffsetDateTime fetchTime, String seqNumber,
            ESMPDateTimeIntervalComplexType periodIntervalTime, List<TimeSeriesComplexType> timeSeriesList) {
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
                .withCreatedDateTime(new EsmpDateTime(fetchTime.toZonedDateTime()).toString())
                .withTimeSeriesList(
                        new ValidatedHistoricalDataMarketDocumentComplexType.TimeSeriesList()
                                .withTimeSeries(timeSeriesList)
                );
    }

    private List<TimeSeriesComplexType> createTimeSeriesList(Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap,
            CommodityKind type, DirectionTypeList flowDirection, String meterId) {
        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
        for(Map.Entry<String, SeriesPeriodComplexType> entry : unitSeriesPeriodMap.entrySet()) {
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
        return unit.equalsIgnoreCase("m3") ? UnitOfMeasureTypeList.CUBIC_METRE : UnitOfMeasureTypeList.fromValue(unit.toUpperCase(Locale.ROOT));
    }

    private List<TimeSeriesComplexType> getTimeSeries(Granularity granularity, ElectricityMeterResponseModel electricityMeterResponse,
            DirectionTypeList flowDirection, boolean offtake) {

        if (granularity.equals(Granularity.P1D)) {
            List<EDailyEnergyItemResponseModel> dailyEnergyList = electricityMeterResponse.getDailyEnergy();
            if (dailyEnergyList == null) {
                return List.of();
            }

            List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
            for (EDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                Map<String, List<PointComplexType>> unitPointsMap = getPoints(dailyEnergyItem, offtake);
                Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(granularity,
                        dailyEnergyItem.getTimestampStart(), dailyEnergyItem.getTimestampEnd(), unitPointsMap);
                timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, CommodityKind.ELECTRICITYPRIMARYMETERED,
                        flowDirection, electricityMeterResponse.getMeterID()));
            }

            return timeSeriesList;
        }

        if(granularity.equals(Granularity.PT15M)) {
            List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergyList = electricityMeterResponse.getQuarterHourlyEnergy();
            if (quarterHourlyEnergyList == null) {
                return List.of();
            }

            List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
            for (EQuarterHourlyEnergyItemResponseModel quarterHourlyEnergyItem : quarterHourlyEnergyList) {
                Map<String, List<PointComplexType>> unitPointsMap = getPoints(quarterHourlyEnergyItem, offtake);
                Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(granularity,
                        quarterHourlyEnergyItem.getTimestampStart(), quarterHourlyEnergyItem.getTimestampEnd(), unitPointsMap);
                timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, CommodityKind.ELECTRICITYPRIMARYMETERED,
                        flowDirection, electricityMeterResponse.getMeterID()));
            }

            return timeSeriesList;
        }

        return List.of();
    }

    private List<TimeSeriesComplexType> getTimeSeries(Granularity granularity, GasMeterResponseModel gasMeterResponse) {

        if (granularity.equals(Granularity.P1D)) {
            List<GDailyEnergyItemResponseModel> dailyEnergyList = gasMeterResponse.getDailyEnergy();
            if (dailyEnergyList == null) {
                return List.of();
            }

            List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
            for (GDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                Map<String, List<PointComplexType>> unitPointsMap = getPoints(dailyEnergyItem);
                Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(granularity,
                        dailyEnergyItem.getTimestampStart(), dailyEnergyItem.getTimestampEnd(), unitPointsMap);
                timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, CommodityKind.NATURALGAS,
                        DirectionTypeList.DOWN, gasMeterResponse.getMeterID()));
            }

            return timeSeriesList;
        }

        if(granularity.equals(Granularity.PT1H)) {
            List<GHourlyEnergyItemResponseModel> hourlyEnergyList = gasMeterResponse.getHourlyEnergy();
            if (hourlyEnergyList == null) {
                return List.of();
            }

            List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
            for (GHourlyEnergyItemResponseModel hourlyEnergyItem : hourlyEnergyList) {
                Map<String, List<PointComplexType>> unitPointsMap = getPoints(hourlyEnergyItem);
                Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(granularity,
                        hourlyEnergyItem.getTimestampStart(), hourlyEnergyItem.getTimestampEnd(), unitPointsMap);
                timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, CommodityKind.NATURALGAS,
                        DirectionTypeList.DOWN, gasMeterResponse.getMeterID()));
            }
            return timeSeriesList;
        }

        return List.of();
    }

    private Map<String, SeriesPeriodComplexType> getSeriesPeriods(Granularity resolution,
            OffsetDateTime timestampStart, OffsetDateTime timestampEnd, Map<String, List<PointComplexType>> unitPointsMap) {
        Map<String, SeriesPeriodComplexType> seriesPeriodMap = new HashMap<>();
        for (Map.Entry<String, List<PointComplexType>> entry : unitPointsMap.entrySet()) {
            seriesPeriodMap.put(entry.getKey(), createSeriesPeriod(resolution.toString(), timestampStart, timestampEnd, entry.getValue()));
        }

        return seriesPeriodMap;
    }

    private Map<String, List<PointComplexType>> getPoints(EQuarterHourlyEnergyItemResponseModel response, boolean offtake) {
        List<EMeasurementDetailItemResponseModel> measurements = response.getMeasurement();

        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();
        for(int i = 0; i < measurements.size(); i++) {
            EMeasurementDetailItemResponseModel measurement = measurements.get(i);
            OffsetDateTime timestampStart = response.getTimestampStart();
            Long position = timestampStart != null ? timestampStart.plusMinutes(15L * i).toEpochSecond() : null;
            String unit =  measurement.getUnit();
            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(position, getQuantity(measurement,offtake), getQuality(measurement, offtake)));
        }

        return pointsMap;
    }

    private Map<String, List<PointComplexType>> getPoints(EDailyEnergyItemResponseModel response, boolean offtake) {
        List<EMeasurementItemResponseModel> measurements = response.getMeasurement();

        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();
        for(int i = 0; i < measurements.size(); i++) {
            EMeasurementItemResponseModel measurement = measurements.get(i);
            OffsetDateTime timestampStart = response.getTimestampStart();
            Long position = timestampStart != null ? timestampStart.plusDays(i).toEpochSecond() : null;
            String unit =  measurement.getUnit();
            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(position, getQuantity(measurement, offtake), getQuality(measurement, offtake)));
        }

        return pointsMap;
    }

    private Map<String, List<PointComplexType>> getPoints(GHourlyEnergyItemResponseModel response) {
        List<GMeasurementDetailItemResponseModel> measurements = response.getMeasurement();

        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();
        for(int i = 0; i < measurements.size(); i++) {
            GMeasurementDetailItemResponseModel measurement = measurements.get(i);
            OffsetDateTime timestampStart = response.getTimestampStart();
            Long position = timestampStart != null ? timestampStart.plusHours(i).toEpochSecond() : null;
            String unit =  measurement.getUnit();
            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(position, measurement.getOfftakeValue(), getQuality(measurement.getOfftakeValidationState())));
        }

        return pointsMap;
    }

    private Map<String, List<PointComplexType>> getPoints(GDailyEnergyItemResponseModel response) {
        List<GMeasurementItemResponseModel> measurements = response.getMeasurement();

        if (measurements == null) {
            return Map.of();
        }

        Map<String, List<PointComplexType>> pointsMap = new HashMap<>();
        for(int i = 0; i < measurements.size(); i++) {
            GMeasurementItemResponseModel measurement = measurements.get(i);
            OffsetDateTime timestampStart = response.getTimestampStart();
            Long position = timestampStart != null ? timestampStart.plusDays(i).toEpochSecond() : null;
            String unit =  measurement.getUnit();
            pointsMap.putIfAbsent(unit, new ArrayList<>());
            pointsMap.get(unit).add(createPoint(position, measurement.getOfftakeValue(), getQuality(measurement.getOfftakeValidationState())));
        }

        return pointsMap;
    }

    private PointComplexType createPoint(Long position, Double quantity, QualityTypeList quality) {
        return new PointComplexType()
                .withPosition(position == null ? null : String.valueOf(position))
                .withEnergyQuantityQuantity(quality == null ? null : BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(quality);
    }

    private Double getQuantity(EMeasurementDetailItemResponseModel measurement, boolean offtake) {
        return offtake ? measurement.getOfftakeValue() : measurement.getInjectionValue();
    }

    private Double getQuantity(EMeasurementItemResponseModel measurement, boolean offtake) {
        return offtake ? sumNullSafe(measurement.getOfftakeDayValue(), measurement.getOfftakeNightValue()) :
                         sumNullSafe(measurement.getInjectionDayValue(), measurement.getInjectionNightValue());
    }

    private Double sumNullSafe(@Nullable Double first, @Nullable Double second) {
        return Optional.ofNullable(first).orElse(0.0) + Optional.ofNullable(second).orElse(0.0);
    }

    private QualityTypeList getQuality(String validationState) {
        return Objects.equals(validationState, EST) ? QualityTypeList.ESTIMATED : QualityTypeList.AS_PROVIDED;
    }

    private QualityTypeList getQuality(EMeasurementDetailItemResponseModel quarterHourlyMeasurement, boolean offtake) {
        String validationState = offtake ? quarterHourlyMeasurement.getOfftakeValidationState() :
                                           quarterHourlyMeasurement.getInjectionValidationState();

        return getQuality(validationState);
    }

    private QualityTypeList getQuality(EMeasurementItemResponseModel dailyMeasurement, boolean offtake) {
        String dayValidationState;
        String nightValidationState;

        if(offtake) {
            dayValidationState = dailyMeasurement.getOfftakeDayValidationState();
            nightValidationState = dailyMeasurement.getOfftakeNightValidationState();
        } else {
            dayValidationState = dailyMeasurement.getInjectionDayValidationState();
            nightValidationState = dailyMeasurement.getInjectionNightValidationState();
        }

        return Objects.equals(dayValidationState, EST) || Objects.equals(nightValidationState, EST) ?
                   QualityTypeList.ESTIMATED : QualityTypeList.AS_PROVIDED;
    }

    private SeriesPeriodComplexType createSeriesPeriod(String resolution, OffsetDateTime timestampStart,
            OffsetDateTime timestampEnd, List<PointComplexType> points) {
        EsmpTimeInterval interval = new EsmpTimeInterval(timestampStart.toZonedDateTime(), timestampEnd.toZonedDateTime());
        return new SeriesPeriodComplexType()
                .withResolution(resolution)
                .withTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                )
                .withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));
    }

    private DirectionTypeList getFlowDirection(boolean offtakeDayValue, boolean injectionDayValue, boolean offtakeValue, boolean injectionValue) {
        if(offtakeValue || offtakeDayValue) {
            return injectionValue || injectionDayValue ? DirectionTypeList.UP_AND_DOWN : DirectionTypeList.DOWN;
        } else {
            return DirectionTypeList.UP;
        }
    }

    private <T, U> boolean checkEnergyMeasurementValue(List<T> response, Function<T, List<U>> measurementExtractor,
            Predicate<U> valueChecker) {

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
    private BusinessTypeList getBusinessType(DirectionTypeList direction) {
        return switch (direction) {
            case UP -> BusinessTypeList.PRODUCTION;
            case DOWN -> BusinessTypeList.CONSUMPTION;
            case UP_AND_DOWN -> BusinessTypeList.NET_PRODUCTION__CONSUMPTION;
            default -> null;
        };
    }

    private ESMPDateTimeIntervalComplexType getPeriodTimeInterval(Granularity granularity, ElectricityMeterResponseModel electricityMeterResponse) {
        ESMPDateTimeIntervalComplexType periodTimeInterval = new ESMPDateTimeIntervalComplexType();
        OffsetDateTime earliestTimestampStart = null;
        OffsetDateTime latestTimestampEnd = null;

        if(granularity.equals(Granularity.P1D)) {
            List<EDailyEnergyItemResponseModel> responses = electricityMeterResponse.getDailyEnergy();
            if (responses == null || responses.isEmpty()) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (EDailyEnergyItemResponseModel response : responses) {
                if(checkEarliestTimestamp(response.getTimestampStart(), earliestTimestampStart)) {
                    earliestTimestampStart = response.getTimestampStart();
                }

                if(checkLatestTimestamp(response.getTimestampEnd(), latestTimestampEnd)) {
                    latestTimestampEnd = response.getTimestampEnd();
                }
            }
        }

        if(granularity.equals(Granularity.PT15M)) {
            List<EQuarterHourlyEnergyItemResponseModel> responses = electricityMeterResponse.getQuarterHourlyEnergy();
            if (responses == null || responses.isEmpty()) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (EQuarterHourlyEnergyItemResponseModel response : responses) {
                if(checkEarliestTimestamp(response.getTimestampStart(), earliestTimestampStart)) {
                    earliestTimestampStart = response.getTimestampStart();
                }

                if(checkLatestTimestamp(response.getTimestampEnd(), latestTimestampEnd)) {
                    latestTimestampEnd = response.getTimestampEnd();
                }
            }
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart.toZonedDateTime(), latestTimestampEnd.toZonedDateTime());
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }

    private ESMPDateTimeIntervalComplexType getPeriodTimeInterval(Granularity granularity, GasMeterResponseModel gasMeterResponse) {
        ESMPDateTimeIntervalComplexType periodTimeInterval = new ESMPDateTimeIntervalComplexType();
        OffsetDateTime earliestTimestampStart = null;
        OffsetDateTime latestTimestampEnd = null;

        if(granularity.equals(Granularity.P1D)) {
            List<GDailyEnergyItemResponseModel> responses = gasMeterResponse.getDailyEnergy();
            if (responses == null || responses.isEmpty()) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (GDailyEnergyItemResponseModel response : responses) {
                if(checkEarliestTimestamp(response.getTimestampStart(), earliestTimestampStart)) {
                    earliestTimestampStart = response.getTimestampStart();
                }

                if(checkLatestTimestamp(response.getTimestampEnd(), latestTimestampEnd)) {
                    latestTimestampEnd = response.getTimestampEnd();
                }
            }
        }

        if(granularity.equals(Granularity.PT1H)) {
            List<GHourlyEnergyItemResponseModel> responses = gasMeterResponse.getHourlyEnergy();
            if (responses == null || responses.isEmpty()) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (GHourlyEnergyItemResponseModel response : responses) {
                if(checkEarliestTimestamp(response.getTimestampStart(), earliestTimestampStart)) {
                    earliestTimestampStart = response.getTimestampStart();
                }

                if(checkLatestTimestamp(response.getTimestampEnd(), latestTimestampEnd)) {
                    latestTimestampEnd = response.getTimestampEnd();
                }
            }
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart.toZonedDateTime(), latestTimestampEnd.toZonedDateTime());
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }

    private boolean checkEarliestTimestamp(OffsetDateTime timestamp, OffsetDateTime timestampCheck) {
        return timestamp != null && timestamp.isBefore(timestampCheck);
    }

    private boolean checkLatestTimestamp(OffsetDateTime timestamp, OffsetDateTime timestampCheck) {
        return timestamp != null && timestamp.isAfter(timestampCheck);
    }
}
