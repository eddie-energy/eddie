package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;

import java.util.UUID;

public class TimeSeriesBuilder {


    private final TimeSeriesComplexType timeSeries = new TimeSeriesComplexType()
            .withMRID(UUID.randomUUID().toString())
            .withProduct(EnergyProductTypeList.ACTIVE_ENERGY);

    public TimeSeriesBuilder withConsumptionRecord(EdaConsumptionRecord consumptionRecord) {
        timeSeries
                .withVersion(consumptionRecord.schemaVersion())
                .withRegistrationDateAndOrTimeDateTime(
                        new DateAndOrTimeComplexType()
                                .withDate(consumptionRecord.processDate())
                )
                .withMarketEvaluationPointMRID(
                        new MeasurementPointIDStringComplexType()
                                .withValue(consumptionRecord.meteringPoint())
                                .withCodingScheme(CodingSchemeTypeList.AUSTRIA_NATIONAL_CODING_SCHEME)
                );
        return this;
    }

    public TimeSeriesBuilder withEnergy(Energy energy) {
        timeSeries
                .withReasonList(
                        new TimeSeriesComplexType.ReasonList()
                                .withReasons(
                                        new ReasonComplexType()
                                                .withText(energy.meteringReason())
                                                .withCode(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED)
                                )
                );
        return this;
    }

    public TimeSeriesBuilder withEnergyData(EnergyData energyData) throws InvalidMappingException {
        timeSeries
                .withEnergyMeasurementUnitName(unitOfMeasureTypeList(energyData))
                .withRegisteredResource(
                        new RegisteredResourceComplexType()
                                .withMRID(energyData.meterCode())
                                .withResourceCapacityList(new RegisteredResourceComplexType.ResourceCapacityList())
                );

        // The available meter/OBIS codes can be found on https://www.ebutilities.at/documents/20200304112759_MeterCodes_ConsumptionRecord.pdf
        // a code looks like this 1-1:1.9.0 P.01
        // Looking at the document, we can use the 5th character to determine if the meter is a consumption or production meter
        if (energyData.meterCode().charAt(4) == '1') { // Consumption
            timeSeries.setBusinessType(BusinessTypeList.CONSUMPTION);
            timeSeries.setFlowDirectionDirection(DirectionTypeList.DOWN);
        } else if (energyData.meterCode().charAt(4) == '2') { // Production
            timeSeries.setBusinessType(BusinessTypeList.PRODUCTION);
            timeSeries.setFlowDirectionDirection(DirectionTypeList.UP);
        } else {
            throw new InvalidMappingException("Unexpected meter code found :" + energyData.meterCode());
        }

        return this;
    }

    private UnitOfMeasureTypeList unitOfMeasureTypeList(EnergyData energyData) throws InvalidMappingException {
        try {
            return UnitOfMeasureTypeList.fromValue(energyData.billingUnit());
        } catch (IllegalArgumentException e) {
            throw new InvalidMappingException("Cant map to UnitOfMeasureTypeList from UOM: " + energyData.billingUnit());
        }
    }

    public TimeSeriesBuilder withSeriesPeriod(SeriesPeriodComplexType seriesPeriod) {
        timeSeries.withSeriesPeriodList(new TimeSeriesComplexType.SeriesPeriodList().withSeriesPeriods(seriesPeriod));
        return this;
    }

    public TimeSeriesComplexType build() {
        return timeSeries;
    }
}
