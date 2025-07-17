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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

class IntermediateValidatedHistoricalDocument {

    private final FluviusOAuthConfiguration fluviusConfig;
    private final IdentifiableMeteringData identifiableMeteredData;
    private final FluviusPermissionRequest fluviusPermissionRequest;
    private final DataNeedsService dataNeedsService;
    private static final String EST = "EST";
    private static final String READ = "READ";

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
        ValidatedHistoricalDataDataNeed dataNeed = ((ValidatedHistoricalDataDataNeed) dataNeedsService.getById(identifiableMeteredData.permissionRequest().dataNeedId()));
        
        if (meteringData == null || dataNeed == null) {
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

    private List<ValidatedHistoricalDataEnvelope> getGasVHD(List<GasMeterResponseModel> gasMeteringData, OffsetDateTime fetchTime) {

        if(gasMeteringData == null) {
            return List.of();
        }

        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(GasMeterResponseModel meterResponse : gasMeteringData ) {
            boolean offtakeDayValue = checkGDailyEnergyMeasurementValue(meterResponse.getDailyEnergy());
            boolean offtakeValue = checkGHourlyEnergyMeasurementValue(meterResponse.getHourlyEnergy());
            // No injection value in gas measurements
            DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue, false, offtakeValue, false);
            String resolution = String.valueOf(getResolution(offtakeDayValue, false, offtakeValue, false));
            String seqNumber = String.valueOf(meterResponse.getSeqNumber());
            Granularity granularity = fluviusPermissionRequest.granularity();
            ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime, seqNumber , periodIntervalTime,
                    getTimeSeries(granularity, meterResponse, resolution, flowDirection));
            vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
        }
        return vhds;
    }

    private List<ValidatedHistoricalDataEnvelope> getElectricityVHD(List<ElectricityMeterResponseModel> electricityMeteringData, OffsetDateTime fetchTime) {

        if(electricityMeteringData == null) {
            return List.of();
        }
        
        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(ElectricityMeterResponseModel meterResponse : electricityMeteringData) {
            boolean offtakeDayValue = checkEDailyEnergyMeasurementValue(meterResponse.getDailyEnergy(), true);
            boolean injectionDayValue = checkEDailyEnergyMeasurementValue(meterResponse.getDailyEnergy(), false);
            boolean offtakeValue = checkEQuarterHourlyEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(), true);
            boolean injectionValue = checkEQuarterHourlyEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(), false);
            DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue, injectionDayValue, offtakeValue, injectionValue);
            String resolution = String.valueOf(getResolution(offtakeDayValue, injectionDayValue, offtakeValue, injectionValue));
            String seqNumber = String.valueOf(meterResponse.getSeqNumber());
            Granularity granularity = fluviusPermissionRequest.granularity();
            ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
            boolean offtake = true;
            if (flowDirection != null && flowDirection.equals(DirectionTypeList.UP)) {
                offtake = false;
            }
            ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(fetchTime, seqNumber , periodIntervalTime,
                    getTimeSeries(granularity, meterResponse, resolution, flowDirection, offtake));
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
            DirectionTypeList flowDirection, String meterId) {
        List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
        for(Map.Entry<String, SeriesPeriodComplexType> entry : unitSeriesPeriodMap.entrySet()) {
            timeSeriesList.add(
                    new TimeSeriesComplexType()
                            .withMRID(UUID.randomUUID().toString())
                            .withBusinessType(getBusinessType(flowDirection))
                            .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                            .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                            .withFlowDirectionDirection(flowDirection)
                            .withEnergyMeasurementUnitName(UnitOfMeasureTypeList.fromValue(getUnitCIM(entry.getKey())))
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

    private String getUnitCIM(String unit) {
        return switch (unit) {
            case "m3", "M3" -> "MTQ";
            default -> unit.toUpperCase(Locale.ROOT);
        };
    }

    private List<TimeSeriesComplexType> getTimeSeries(Granularity granularity,
            ElectricityMeterResponseModel electricityMeterResponse, String resolution, DirectionTypeList flowDirection, boolean offtake) {

        if (granularity.equals(Granularity.P1D)) {
            List<EDailyEnergyItemResponseModel> dailyEnergyList = electricityMeterResponse.getDailyEnergy();
            if (dailyEnergyList != null) {
                List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
                for (EDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                    Map<String, List<PointComplexType>> unitPointsMap = getPoints(dailyEnergyItem, offtake);
                    Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(resolution,
                            dailyEnergyItem.getTimestampStart(), dailyEnergyItem.getTimestampEnd(), unitPointsMap);
                    timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, flowDirection, electricityMeterResponse.getMeterID()));
                }
                return timeSeriesList;
            }
        }

        if(granularity.equals(Granularity.PT15M)) {
            List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergyList = electricityMeterResponse.getQuarterHourlyEnergy();
            if (quarterHourlyEnergyList != null) {
                List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
                for (EQuarterHourlyEnergyItemResponseModel quarterHourlyEnergyItem : quarterHourlyEnergyList) {
                    Map<String, List<PointComplexType>> unitPointsMap = getPoints(quarterHourlyEnergyItem, offtake);
                    Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(resolution,
                            quarterHourlyEnergyItem.getTimestampStart(), quarterHourlyEnergyItem.getTimestampEnd(), unitPointsMap);
                    timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, flowDirection, electricityMeterResponse.getMeterID()));
                }
                return timeSeriesList;
            }
        }

        return List.of();
    }

    private List<TimeSeriesComplexType> getTimeSeries(Granularity granularity,
            GasMeterResponseModel gasMeterResponse, String resolution, DirectionTypeList flowDirection) {

        if (granularity.equals(Granularity.P1D)) {
            List<GDailyEnergyItemResponseModel> dailyEnergyList = gasMeterResponse.getDailyEnergy();
            if (dailyEnergyList != null) {
                List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
                for (GDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                    Map<String, List<PointComplexType>> unitPointsMap = getPoints(dailyEnergyItem);
                    Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(resolution,
                            dailyEnergyItem.getTimestampStart(), dailyEnergyItem.getTimestampEnd(), unitPointsMap);
                    timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, flowDirection, gasMeterResponse.getMeterID()));
                }
                return timeSeriesList;
            }
        }

        if(granularity.equals(Granularity.PT1H)) {
            List<GHourlyEnergyItemResponseModel> hourlyEnergyList = gasMeterResponse.getHourlyEnergy();
            if (hourlyEnergyList != null) {
                List<TimeSeriesComplexType> timeSeriesList = new ArrayList<>();
                for (GHourlyEnergyItemResponseModel hourlyEnergyItem : hourlyEnergyList) {
                    Map<String, List<PointComplexType>> unitPointsMap = getPoints(hourlyEnergyItem);
                    Map<String, SeriesPeriodComplexType> unitSeriesPeriodMap = getSeriesPeriods(resolution,
                            hourlyEnergyItem.getTimestampStart(), hourlyEnergyItem.getTimestampEnd(), unitPointsMap);
                    timeSeriesList.addAll(createTimeSeriesList(unitSeriesPeriodMap, flowDirection, gasMeterResponse.getMeterID()));
                }
                return timeSeriesList;
            }
        }

        return List.of();
    }

    private Map<String, SeriesPeriodComplexType> getSeriesPeriods(String resolution,
            OffsetDateTime timestampStart, OffsetDateTime timestampEnd, Map<String, List<PointComplexType>> unitPointsMap) {
        Map<String, SeriesPeriodComplexType> seriesPeriodMap = new HashMap<>();
        for (Map.Entry<String, List<PointComplexType>> entry : unitPointsMap.entrySet()) {
            seriesPeriodMap.put(entry.getKey(), createSeriesPeriod(resolution, timestampStart, timestampEnd, entry.getValue()));
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
            if(!pointsMap.containsKey(unit)) {
                pointsMap.put(unit, new ArrayList<>());
            }
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
            if(!pointsMap.containsKey(unit)) {
                pointsMap.put(unit, new ArrayList<>());
            }
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
            if(!pointsMap.containsKey(unit)) {
                pointsMap.put(unit, new ArrayList<>());
            }
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
            if(!pointsMap.containsKey(unit)) {
                pointsMap.put(unit, new ArrayList<>());
            }
            pointsMap.get(unit).add(createPoint(position, measurement.getOfftakeValue(), getQuality(measurement.getOfftakeValidationState())));
        }

        return pointsMap;
    }

    private PointComplexType createPoint(Long position, Double quantity, String quality) {
        return new PointComplexType()
                .withPosition(String.valueOf(position))
                .withEnergyQuantityQuantity(BigDecimal.valueOf(quantity))
                .withEnergyQuantityQuality(getQualityType(quality));
    }

    private Double getQuantity(EMeasurementDetailItemResponseModel measurement, boolean offtake) {
        if(offtake) {
            return measurement.getOfftakeValue();
        } else {
            return measurement.getInjectionValue();
        }
    }

    private Double getQuantity(EMeasurementItemResponseModel measurement, boolean offtake) {
        if (offtake) {
            if(measurement.getOfftakeDayValue() != null && measurement.getOfftakeNightValue() != null) {
                return measurement.getOfftakeDayValue() + measurement.getOfftakeNightValue();
            }
        } else {
            if(measurement.getInjectionDayValue() != null && measurement.getInjectionNightValue() != null) {
                return measurement.getInjectionDayValue() + measurement.getInjectionNightValue();
            }
        }
        return null;
    }

    private String getQuality(String offtakeValidationState) {
        if(offtakeValidationState != null && offtakeValidationState.equals(EST)) {
            return EST;
        }

        return READ;
    }

    private String getQuality(EMeasurementDetailItemResponseModel quarterHourlyMeasurement, boolean offtake) {
        String validationState;
        if(offtake) {
            validationState = quarterHourlyMeasurement.getOfftakeValidationState();
        } else {
            validationState = quarterHourlyMeasurement.getInjectionValidationState();
        }
        if (validationState != null && validationState.equals(EST)) {
            return EST;
        }

        return READ;
    }

    private String getQuality(EMeasurementItemResponseModel dailyMeasurement, boolean offtake) {
        String dayValidationState;
        String nightValidationState;

        if(offtake) {
            dayValidationState = dailyMeasurement.getOfftakeDayValidationState();
            nightValidationState = dailyMeasurement.getOfftakeNightValidationState();
        } else {
            dayValidationState = dailyMeasurement.getInjectionDayValidationState();
            nightValidationState = dailyMeasurement.getInjectionNightValidationState();
        }

        return dayValidationState != null && nightValidationState != null &&
               (dayValidationState.equals(EST) || nightValidationState.equals(EST))? EST :  READ;
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

    private QualityTypeList getQualityType(String quality) {
        return switch (quality) {
            case "EST" -> QualityTypeList.ESTIMATED;
            case "READ" -> QualityTypeList.AS_PROVIDED;
            default -> null;
        };
    }

    private DirectionTypeList getFlowDirection(boolean offtakeDayValue, boolean injectionDayValue, boolean offtakeValue, boolean injectionValue) {
        if(offtakeValue || offtakeDayValue) {
            if(injectionValue || injectionDayValue) {
                return DirectionTypeList.UP_AND_DOWN;
            } else {
                return DirectionTypeList.DOWN;
            }
        } else {
            if(injectionValue || injectionDayValue) {
                return DirectionTypeList.UP;
            } else {
                return null;
            }
        }
    }

    private Granularity getResolution(boolean offtakeDayValue, boolean injectionDayValue, boolean offtakeValue, boolean injectionValue) {
        if(offtakeDayValue || injectionDayValue) {
            return Granularity.P1D;
        }

        if(offtakeValue || injectionValue) {
            return Granularity.PT1H;
        }

        return null;
    }

    private boolean checkEDailyEnergyMeasurementValue(List<EDailyEnergyItemResponseModel> response, boolean offtake) {
        if(response == null) {
            return false;
        }

        for(EDailyEnergyItemResponseModel item : response) {
            List<EMeasurementItemResponseModel> eMeasurements = item.getMeasurement();
            if (eMeasurements == null) {
                continue;
            }

            EMeasurementItemResponseModel eMeasurement = eMeasurements.getFirst();
            Double value = getQuantity(eMeasurement, offtake);
            if (value != null && value != 0) {
                return true;
            }
        }

        return false;
    }

    private boolean checkEQuarterHourlyEnergyMeasurementValue(List<EQuarterHourlyEnergyItemResponseModel> response, boolean offtake) {
        if(response == null) {
            return false;
        }

        for(EQuarterHourlyEnergyItemResponseModel item : response) {
            List<EMeasurementDetailItemResponseModel> eMeasurements = item.getMeasurement();
            if (eMeasurements == null) {
                continue;
            }

            for (EMeasurementDetailItemResponseModel eMeasurement : eMeasurements) {
                if (offtake) {
                    Double offtakeValue = eMeasurement.getOfftakeValue();
                    if(offtakeValue != null && offtakeValue != 0) {
                        return true;
                    }
                } else {
                    Double injectionValue = eMeasurement.getInjectionValue();
                    if(injectionValue != null && injectionValue != 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean checkGDailyEnergyMeasurementValue(List<GDailyEnergyItemResponseModel> response) {
        if(response == null) {
            return false;
        }

        for(GDailyEnergyItemResponseModel item : response) {
            List<GMeasurementItemResponseModel> gMeasurements = item.getMeasurement();
            if (gMeasurements == null) {
                continue;
            }

            for (GMeasurementItemResponseModel gMeasurement : gMeasurements) {
                // No offtakeNightValue in gas measurement
                if(gMeasurement.getOfftakeValue() != null && gMeasurement.getOfftakeValue() > 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean checkGHourlyEnergyMeasurementValue(List<GHourlyEnergyItemResponseModel> response) {
        if(response == null) {
            return false;
        }

        for(GHourlyEnergyItemResponseModel item : response) {
            List<GMeasurementDetailItemResponseModel> gMeasurements = item.getMeasurement();
            if (gMeasurements == null) {
                continue;
            }

            for (GMeasurementDetailItemResponseModel gMeasurement : gMeasurements) {
                Double offtakeValue = gMeasurement.getOfftakeValue();
                if(offtakeValue != null && offtakeValue != 0) {
                    return true;
                }
            }
        }

        return false;
    }

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
            if (responses == null) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (EDailyEnergyItemResponseModel response : responses) {
                OffsetDateTime timestampStart = response.getTimestampStart();
                if (timestampStart != null && timestampStart.isBefore(earliestTimestampStart)) {
                    earliestTimestampStart = timestampStart;
                }

                OffsetDateTime timestampEnd = response.getTimestampEnd();
                if (timestampEnd != null && timestampEnd.isAfter(latestTimestampEnd)) {
                    latestTimestampEnd = timestampEnd;
                }
            }
        }

        if(granularity.equals(Granularity.PT15M)) {
            List<EQuarterHourlyEnergyItemResponseModel> responses = electricityMeterResponse.getQuarterHourlyEnergy();
            if (responses == null) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (EQuarterHourlyEnergyItemResponseModel response : responses) {
                OffsetDateTime timestampStart = response.getTimestampStart();
                if (timestampStart != null && timestampStart.isBefore(earliestTimestampStart)) {
                    earliestTimestampStart = timestampStart;
                }

                OffsetDateTime timestampEnd = response.getTimestampEnd();
                if (timestampEnd != null && timestampEnd.isAfter(latestTimestampEnd)) {
                    latestTimestampEnd = timestampEnd;
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
            if (responses == null) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (GDailyEnergyItemResponseModel response : responses) {
                OffsetDateTime timestampStart = response.getTimestampStart();
                if (timestampStart != null && timestampStart.isBefore(earliestTimestampStart)) {
                    earliestTimestampStart = timestampStart;
                }

                OffsetDateTime timestampEnd = response.getTimestampEnd();
                if (timestampEnd != null && timestampEnd.isAfter(latestTimestampEnd)) {
                    latestTimestampEnd = timestampEnd;
                }
            }
        }

        if(granularity.equals(Granularity.PT1H)) {
            List<GHourlyEnergyItemResponseModel> responses = gasMeterResponse.getHourlyEnergy();
            if (responses == null) {
                return periodTimeInterval;
            }

            earliestTimestampStart = responses.getFirst().getTimestampStart();
            latestTimestampEnd = responses.getFirst().getTimestampEnd();
            for (GHourlyEnergyItemResponseModel response : responses) {
                OffsetDateTime timestampStart = response.getTimestampStart();
                if (timestampStart != null && timestampStart.isBefore(earliestTimestampStart)) {
                    earliestTimestampStart = timestampStart;
                }

                OffsetDateTime timestampEnd = response.getTimestampEnd();
                if (timestampEnd != null && timestampEnd.isAfter(latestTimestampEnd)) {
                    latestTimestampEnd = timestampEnd;
                }
            }
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart.toZonedDateTime(), latestTimestampEnd.toZonedDateTime());
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }
}
