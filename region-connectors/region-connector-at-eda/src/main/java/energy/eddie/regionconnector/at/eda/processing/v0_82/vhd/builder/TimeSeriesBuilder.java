package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;

import java.util.UUID;

public class TimeSeriesBuilder {


    private final TimeSeriesComplexType timeSeries = new TimeSeriesComplexType()
            .withMRID(UUID.randomUUID().toString())
            .withProduct(EnergyProductTypeList.ACTIVE_POWER);

    public TimeSeriesBuilder withProcessDirectory(ProcessDirectory processDirectory) {
        timeSeries
                .withRegistrationDateAndOrTimeDateTime(
                        new DateAndOrTimeComplexType()
                                .withDate(processDirectory.getProcessDate())
                )
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withValue(processDirectory.getMeteringPoint())
                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                );
        return this;
    }

    public TimeSeriesBuilder withMarketParticipantDirectory(MarketParticipantDirectory marketParticipantDirectory) {
        timeSeries
                .withVersion(marketParticipantDirectory.getSchemaVersion());

        return this;
    }

    public TimeSeriesBuilder withEnergy(Energy energy) {
        timeSeries
                .withReasonList(
                        new TimeSeriesComplexType.ReasonList()
                                .withReasons(
                                        new ReasonComplexType()
                                                .withText(energy.getMeteringReason())
                                                .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                                )
                );
        return this;
    }

    public TimeSeriesBuilder withEnergyData(EnergyData energyData) throws InvalidMappingException {
        timeSeries
                .withEnergyMeasurementUnitName(fromUOMType(energyData.getUOM()))
                .withRegisteredResource(
                        new RegisteredResourceComplexType()
                                .withMRID(energyData.getMeterCode())
                                .withResourceCapacityList(new RegisteredResourceComplexType.ResourceCapacityList())
                );

        // The available meter/OBIS codes can be found on https://www.ebutilities.at/documents/20200304112759_MeterCodes_ConsumptionRecord.pdf
        // a code looks like this 1-1:1.9.0 P.01
        // Looking at the document, we can use the 5th character to determine if the meter is a consumption or production meter
        if (energyData.getMeterCode().charAt(4) == '1') { // Consumption
            timeSeries.setBusinessType(BusinessTypeList.CONSUMPTION);
            timeSeries.setFlowDirectionDirection(DirectionTypeList.DOWN);
        } else if (energyData.getMeterCode().charAt(4) == '2') { // Production
            timeSeries.setBusinessType(BusinessTypeList.PRODUCTION);
            timeSeries.setFlowDirectionDirection(DirectionTypeList.UP);
        } else {
            throw new InvalidMappingException("Unexpected meter code found :" + energyData.getMeterCode());
        }

        return this;
    }

    public TimeSeriesBuilder withSeriesPeriod(SeriesPeriodComplexType seriesPeriod) {
        timeSeries.withSeriesPeriodList(new TimeSeriesComplexType.SeriesPeriodList().withSeriesPeriods(seriesPeriod));
        return this;
    }

    public TimeSeriesComplexType build() {
        return timeSeries;
    }

    private UnitOfMeasureTypeList fromUOMType(UOMType uomType) throws InvalidMappingException {
        try {
            return UnitOfMeasureTypeList.fromValue(uomType.value());
        } catch (IllegalArgumentException e) {
            throw new InvalidMappingException("Cant map to UnitOfMeasureTypeList from UOM: " + uomType.value());
        }
    }
}