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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class IntermediateValidatedHistoricalDocument {

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
        
        if (meteringData == null) {
            return List.of();
        }
        
        EnergyType type = ((ValidatedHistoricalDataDataNeed) dataNeedsService.getById(identifiableMeteredData.permissionRequest().dataNeedId())).energyType();
        return switch (type) {
            case ELECTRICITY -> getElectricityVHD(meteringData.getElectricityMeters(), meteringData.getFetchTime());
            case NATURAL_GAS -> getGasVHD(meteringData.getGasMeters(), meteringData.getFetchTime());
            default -> throw new IllegalStateException("Unexpected energy type: " + type);
        };
    }

    private List<ValidatedHistoricalDataEnvelope> getGasVHD(List<GasMeterResponseModel> gasMeteringData, OffsetDateTime fetchTime) {

        if(gasMeteringData == null) {
            return List.of();
        }

        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(GasMeterResponseModel meterResponse : gasMeteringData ) {
            if (meterResponse.getSeqNumber() != null) {
                boolean offtakeDayValue = checkGDailyEnergyMeasurementValue(meterResponse.getDailyEnergy());
                boolean offtakeValue = checkGHourlyEnergyMeasurementValue(meterResponse.getHourlyEnergy());
                // No injection value in gas measurements
                DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue, false, offtakeValue, false);
                String resolution = String.valueOf(getResolution(offtakeDayValue, false, offtakeValue, false));
                String seqNumber = meterResponse.getSeqNumber().toString();
                String meterId = meterResponse.getMeterID();
                Granularity granularity = fluviusPermissionRequest.granularity();
                UnitOfMeasureTypeList unit = UnitOfMeasureTypeList.fromValue(getUnit(granularity, meterResponse).toUpperCase());
                ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
                List<SeriesPeriodComplexType> seriesPeriods = getSeriesPeriods(granularity, meterResponse, resolution);

                ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(flowDirection, fetchTime, seqNumber, meterId, unit, periodIntervalTime, seriesPeriods);
                vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
            }
        }
        return vhds;
    }

    private List<ValidatedHistoricalDataEnvelope> getElectricityVHD(List<ElectricityMeterResponseModel> electricityMeteringData, OffsetDateTime fetchTime) {

        if(electricityMeteringData == null) {
            return List.of();
        }
        
        List<ValidatedHistoricalDataEnvelope> vhds = new ArrayList<>();
        for(ElectricityMeterResponseModel meterResponse : electricityMeteringData) {
            if (meterResponse.getSeqNumber() != null) {
                boolean offtakeDayValue = checkEDailyEnergyMeasurementValue(meterResponse.getDailyEnergy(), true);
                boolean injectionDayValue = checkEDailyEnergyMeasurementValue(meterResponse.getDailyEnergy(), false);
                boolean offtakeValue = checkEQuarterHourlyEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(), true);
                boolean injectionValue = checkEQuarterHourlyEnergyMeasurementValue(meterResponse.getQuarterHourlyEnergy(), false);
                DirectionTypeList flowDirection = getFlowDirection(offtakeDayValue, injectionDayValue, offtakeValue, injectionValue);
                String resolution = String.valueOf(getResolution(offtakeDayValue, injectionDayValue, offtakeValue, injectionValue));
                String seqNumber = meterResponse.getSeqNumber().toString();
                String meterId = meterResponse.getMeterID();
                Granularity granularity = fluviusPermissionRequest.granularity();
                UnitOfMeasureTypeList unit = UnitOfMeasureTypeList.fromValue(getUnit(granularity, meterResponse).toUpperCase());
                ESMPDateTimeIntervalComplexType periodIntervalTime = getPeriodTimeInterval(granularity, meterResponse);
                boolean offtake = true;
                if (flowDirection != null && flowDirection.equals(DirectionTypeList.UP)) {
                    offtake = false;
                }
                List<SeriesPeriodComplexType> seriesPeriods = getSeriesPeriods(granularity, meterResponse, resolution, offtake);

                ValidatedHistoricalDataMarketDocumentComplexType vhd = createVHD(flowDirection, fetchTime, seqNumber, meterId, unit, periodIntervalTime, seriesPeriods);
                vhds.add(new VhdEnvelope(vhd, identifiableMeteredData.permissionRequest()).wrap());
            }
        }
        return vhds;
    }

    private ValidatedHistoricalDataMarketDocumentComplexType createVHD(DirectionTypeList flowDirection,
            OffsetDateTime fetchTime, String seqNumber, String meterId, UnitOfMeasureTypeList unit,
            ESMPDateTimeIntervalComplexType periodIntervalTime, List<SeriesPeriodComplexType> seriesPeriods) {
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
                                .withTimeSeries(
                                        new TimeSeriesComplexType()
                                                .withMRID(UUID.randomUUID().toString())
                                                .withBusinessType(getBusinessType(flowDirection))
                                                .withProduct(EnergyProductTypeList.ACTIVE_ENERGY)
                                                .withMarketEvaluationPointMeterReadingsReadingsReadingTypeCommodity(CommodityKind.ELECTRICITYPRIMARYMETERED)
                                                .withFlowDirectionDirection(flowDirection)
                                                .withEnergyMeasurementUnitName(unit)
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
                                                                .withSeriesPeriods(seriesPeriods)
                                                )
                                )
                );
    }

    private List<SeriesPeriodComplexType> getSeriesPeriods(Granularity granularity,
            ElectricityMeterResponseModel electricityMeterResponse, String resolution, boolean offtake) {
        if (electricityMeterResponse != null) {
            if (granularity.equals(Granularity.P1D)) {
                List<EDailyEnergyItemResponseModel> dailyEnergyList = electricityMeterResponse.getDailyEnergy();
                if (dailyEnergyList != null) {
                    List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
                    for (EDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                        seriesPeriods.add(createSeriesPeriod(resolution, dailyEnergyItem.getTimestampStart(),
                                dailyEnergyItem.getTimestampEnd(), getPoints(dailyEnergyItem, offtake)));
                    }
                    return seriesPeriods;
                }
            }

            if(granularity.equals(Granularity.PT15M)) {
                List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergyList = electricityMeterResponse.getQuarterHourlyEnergy();
                if (quarterHourlyEnergyList != null) {
                    List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
                    for (EQuarterHourlyEnergyItemResponseModel quarterHourlyEnergyItem : quarterHourlyEnergyList) {
                        seriesPeriods.add(createSeriesPeriod(resolution, quarterHourlyEnergyItem.getTimestampStart(),
                                quarterHourlyEnergyItem.getTimestampEnd(), getPoints(quarterHourlyEnergyItem, offtake)));
                    }
                    return seriesPeriods;
                }
            }
        }

        return null;
    }

    private List<SeriesPeriodComplexType> getSeriesPeriods(Granularity granularity, GasMeterResponseModel gasMeterResponse, String resolution) {
        if (gasMeterResponse != null) {
            if (granularity.equals(Granularity.P1D)) {
                List<GDailyEnergyItemResponseModel> dailyEnergyList = gasMeterResponse.getDailyEnergy();
                if (dailyEnergyList != null) {
                    List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
                    for (GDailyEnergyItemResponseModel dailyEnergyItem : dailyEnergyList) {
                        seriesPeriods.add(createSeriesPeriod(resolution, dailyEnergyItem.getTimestampStart(),
                                dailyEnergyItem.getTimestampEnd(), getPoints(dailyEnergyItem)));
                    }
                    return seriesPeriods;
                }
            }

            if(granularity.equals(Granularity.PT1H)) {
                List<GHourlyEnergyItemResponseModel> hourlyEnergyList = gasMeterResponse.getHourlyEnergy();
                if (hourlyEnergyList != null) {
                    List<SeriesPeriodComplexType> seriesPeriods = new ArrayList<>();
                    for (GHourlyEnergyItemResponseModel hourlyEnergyItem : hourlyEnergyList) {
                            seriesPeriods.add(createSeriesPeriod(resolution, hourlyEnergyItem.getTimestampStart(),
                                    hourlyEnergyItem.getTimestampEnd(), getPoints(hourlyEnergyItem)));
                    }
                    return seriesPeriods;
                }
            }
        }

        return null;
    }

    private List<PointComplexType> getPoints(EQuarterHourlyEnergyItemResponseModel response, boolean offtake) {
        List<PointComplexType> points = new ArrayList<>();
        List<EMeasurementDetailItemResponseModel> measurements = response.getMeasurement();
        if (measurements != null) {
            for(int i = 0; i < measurements.size(); i++) {
                EMeasurementDetailItemResponseModel measurement = measurements.get(i);
                OffsetDateTime timestampStart = response.getTimestampStart();
                Long position = timestampStart != null ? timestampStart.plusMinutes(15L * i).toEpochSecond() : null;
                points.add(createPoint(position, getQuantity(measurement,offtake), getQuality(measurement, offtake)));
            }
        }

        return points;
    }

    private List<PointComplexType> getPoints(EDailyEnergyItemResponseModel response, boolean offtake) {
        List<PointComplexType> points = new ArrayList<>();
        List<EMeasurementItemResponseModel> measurements = response.getMeasurement();
        if (measurements != null) {
            for(int i = 0; i < measurements.size(); i++) {
                EMeasurementItemResponseModel measurement = measurements.get(i);
                OffsetDateTime timestampStart = response.getTimestampStart();
                Long position = timestampStart != null ? timestampStart.plusDays(i).toEpochSecond() : null;
                points.add(createPoint(position, getQuantity(measurement, offtake), getQuality(measurement, offtake)));
            }
        }

        return points;
    }

    private List<PointComplexType> getPoints(GHourlyEnergyItemResponseModel response) {
        List<PointComplexType> points = new ArrayList<>();
        List<GMeasurementDetailItemResponseModel> measurements = response.getMeasurement();
        if (measurements != null) {
            for(int i = 0; i < measurements.size(); i++) {
                GMeasurementDetailItemResponseModel measurement = measurements.get(i);
                OffsetDateTime timestampStart = response.getTimestampStart();
                Long position = timestampStart != null ? timestampStart.plusHours(i).toEpochSecond() : null;
                points.add(createPoint(position, measurement.getOfftakeValue(), getQuality(measurement.getOfftakeValidationState())));
            }
        }

        return  points;
    }

    private List<PointComplexType> getPoints(GDailyEnergyItemResponseModel response) {
        List<PointComplexType> points = new ArrayList<>();
        List<GMeasurementItemResponseModel> measurements = response.getMeasurement();
        if (measurements != null) {
            for(int i = 0; i < measurements.size(); i++) {
                GMeasurementItemResponseModel measurement = measurements.get(i);
                OffsetDateTime timestampStart = response.getTimestampStart();
                Long position = timestampStart != null ? timestampStart.plusDays(i).toEpochSecond() : null;
                points.add(createPoint(position, measurement.getOfftakeValue(), getQuality(measurement.getOfftakeValidationState())));
            }
        }

        return  points;
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
            OffsetDateTime timestampEnd) {
        EsmpTimeInterval interval = new EsmpTimeInterval(timestampStart.toZonedDateTime(), timestampEnd.toZonedDateTime());
        return new SeriesPeriodComplexType()
                .withResolution(resolution)
                .withTimeInterval(
                        new ESMPDateTimeIntervalComplexType()
                                .withStart(interval.start())
                                .withEnd(interval.end())
                );
    }

    private SeriesPeriodComplexType createSeriesPeriod(String resolution, OffsetDateTime timestampStart,
            OffsetDateTime timestampEnd, List<PointComplexType> points) {
        return createSeriesPeriod(resolution, timestampStart, timestampEnd)
                .withPointList(new SeriesPeriodComplexType.PointList().withPoints(points));
    }

    private QualityTypeList getQualityType(String quality) {
        return switch (quality) {
            case "EST" -> QualityTypeList.ESTIMATED;
            case "READ" -> QualityTypeList.AS_PROVIDED;
            default -> null;
        };
    }

    private String getUnit(Granularity granularity, GasMeterResponseModel gasMeterResponse) {
        if (gasMeterResponse != null) {
            if(granularity.equals(Granularity.P1D)) {
                List<GDailyEnergyItemResponseModel> dailyEnergyList = gasMeterResponse.getDailyEnergy();
                if (dailyEnergyList != null && !dailyEnergyList.isEmpty()) {
                    List<GMeasurementItemResponseModel> measurements = dailyEnergyList.getFirst().getMeasurement();
                    if (measurements != null && !measurements.isEmpty()) {
                        return measurements.getFirst().getUnit();
                    }
                }
            }

            if(granularity.equals(Granularity.PT1H)) {
                List<GHourlyEnergyItemResponseModel> hourlyEnergyList = gasMeterResponse.getHourlyEnergy();
                if (hourlyEnergyList != null && !hourlyEnergyList.isEmpty()) {
                    List<GMeasurementDetailItemResponseModel> measurements = hourlyEnergyList.getFirst().getMeasurement();
                    if (measurements != null && !measurements.isEmpty()) {
                        return measurements.getFirst().getUnit();
                    }
                }
            }
        }

        return null;
    }

    private String getUnit(Granularity granularity, ElectricityMeterResponseModel electricityMeterResponse) {
        if (electricityMeterResponse != null) {
            if(granularity.equals(Granularity.P1D)) {
                List<EDailyEnergyItemResponseModel> dailyEnergyList = electricityMeterResponse.getDailyEnergy();
                if (dailyEnergyList != null && !dailyEnergyList.isEmpty()) {
                    List<EMeasurementItemResponseModel> measurements = dailyEnergyList.getFirst().getMeasurement();
                    if (measurements != null) {
                        return measurements.getFirst().getUnit();
                    }
                }
            }

            if(granularity.equals(Granularity.PT15M)) {
                List<EQuarterHourlyEnergyItemResponseModel> quarterHourlyEnergyList = electricityMeterResponse.getQuarterHourlyEnergy();
                if (quarterHourlyEnergyList != null && !quarterHourlyEnergyList.isEmpty()) {
                    List<EMeasurementDetailItemResponseModel> measurements = quarterHourlyEnergyList.getFirst().getMeasurement();
                    if (measurements != null) {
                        return measurements.getFirst().getUnit();
                    }
                }
            }
        }

        return null;
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
        if(response != null) {
            for(EDailyEnergyItemResponseModel item : response) {
                List<EMeasurementItemResponseModel> eMeasurements = item.getMeasurement();
                if (eMeasurements != null) {
                    EMeasurementItemResponseModel eMeasurement = eMeasurements.getFirst();
                    Double value = getQuantity(eMeasurement, offtake);
                    if (value != null && value != 0) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean checkEQuarterHourlyEnergyMeasurementValue(List<EQuarterHourlyEnergyItemResponseModel> response, boolean offtake) {
        if(response != null) {
            for(EQuarterHourlyEnergyItemResponseModel item : response) {
                List<EMeasurementDetailItemResponseModel> eMeasurements = item.getMeasurement();
                if (eMeasurements != null) {
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
            }
        }

        return false;
    }

    private boolean checkGDailyEnergyMeasurementValue(List<GDailyEnergyItemResponseModel> response) {
        if(response != null) {
            for(GDailyEnergyItemResponseModel item : response) {
                List<GMeasurementItemResponseModel> gMeasurements = item.getMeasurement();
                if (gMeasurements != null) {
                    for (GMeasurementItemResponseModel gMeasurement : gMeasurements) {
                        // No offtakeNightValue in gas measurement
                        if(gMeasurement.getOfftakeValue() != null && gMeasurement.getOfftakeValue() > 0) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean checkGHourlyEnergyMeasurementValue(List<GHourlyEnergyItemResponseModel> response) {
        if(response != null) {
            for(GHourlyEnergyItemResponseModel item : response) {
                List<GMeasurementDetailItemResponseModel> gMeasurements = item.getMeasurement();
                if (gMeasurements != null) {
                    for (GMeasurementDetailItemResponseModel gMeasurement : gMeasurements) {
                        Double offtakeValue = gMeasurement.getOfftakeValue();
                        if(offtakeValue != null && offtakeValue != 0) {
                            return true;
                        }
                    }
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
            case null, default -> null;
        };
    }

    private ESMPDateTimeIntervalComplexType getPeriodTimeInterval(Granularity granularity, ElectricityMeterResponseModel electricityMeterResponse) {
        ESMPDateTimeIntervalComplexType periodTimeInterval = new ESMPDateTimeIntervalComplexType();
        OffsetDateTime earliestTimestampStart = null;
        OffsetDateTime latestTimestampEnd = null;

        if(granularity.equals(Granularity.P1D)) {
            List<EDailyEnergyItemResponseModel> responses = electricityMeterResponse.getDailyEnergy();
            if (responses != null) {
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
        }

        if(granularity.equals(Granularity.PT15M)) {
            List<EQuarterHourlyEnergyItemResponseModel> responses = electricityMeterResponse.getQuarterHourlyEnergy();
            if (responses != null) {
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
            if (responses != null) {
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
        }

        if(granularity.equals(Granularity.PT1H)) {
            List<GHourlyEnergyItemResponseModel> responses = gasMeterResponse.getHourlyEnergy();
            if (responses != null) {
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
        }

        if (earliestTimestampStart == null || latestTimestampEnd == null) {
            return periodTimeInterval;
        }

        EsmpTimeInterval interval = new EsmpTimeInterval(earliestTimestampStart.toZonedDateTime(), latestTimestampEnd.toZonedDateTime());
        return periodTimeInterval.withStart(interval.start()).withEnd(interval.end());
    }
}
