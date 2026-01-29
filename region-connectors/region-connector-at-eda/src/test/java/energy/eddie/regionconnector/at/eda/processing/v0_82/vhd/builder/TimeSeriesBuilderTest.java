// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.cim.v0_82.vhd.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesBuilderTest {

    @Test
    void build_afterConstruction() {
        var timeSeries = new TimeSeriesBuilder().build();

        assertNull(timeSeries.getSeriesPeriodList());
        assertNull(timeSeries.getReasonList());
        assertNull(timeSeries.getRegistrationDateAndOrTimeDateTime());
        assertNull(timeSeries.getMarketEvaluationPointMRID());
        assertNotNull(timeSeries.getMRID());
        assertEquals(EnergyProductTypeList.ACTIVE_ENERGY, timeSeries.getProduct());
    }

    @Test
    void withConsumptionRecord_setsDateAndMarketEvaluationPoint() {
        String meteringPoint = "meteringPoint";
        String version = "version";
        XMLGregorianCalendar processDate = DateTimeConverter.dateToXml(LocalDate.of(2023, 1, 1));
        EdaConsumptionRecord consumptionRecord = new SimpleEdaConsumptionRecord()
                .setSchemaVersion(version)
                .setProcessDate(processDate)
                .setMeteringPoint(meteringPoint);

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withConsumptionRecord(consumptionRecord);
        var timeSeries = uut.build();

        assertAll(
                () -> assertEquals(meteringPoint, timeSeries.getMarketEvaluationPointMRID().getValue()),
                () -> assertEquals(processDate, timeSeries.getRegistrationDateAndOrTimeDateTime().getDate()),
                () -> assertEquals(version, timeSeries.getVersion())
        );
    }

    @Test
    void withEnergy_setsReason() {
        String meteringReason = "meteringReason";
        Energy energy = new SimpleEnergy()
                .setMeteringReason(meteringReason);

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withEnergy(energy);
        var timeSeries = uut.build();

        assertEquals(meteringReason, timeSeries.getReasonList().getReasons().getFirst().getText());
        assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED,
                     timeSeries.getReasonList().getReasons().getFirst().getCode());
    }

    @Test
    void withEnergyData_withConsumptionMeterCode_setsMeteringInformation() throws InvalidMappingException {
        String meterCode = "1-1:1.9.0 P.01"; // Consumption meter
        EnergyData energyData = new SimpleEnergyData()
                .setMeterCode(meterCode)
                .setBillingUnit("KWH");

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withEnergyData(energyData);
        var timeSeries = uut.build();

        assertEquals(meterCode, timeSeries.getRegisteredResource().getMRID());
        assertEquals(UnitOfMeasureTypeList.KILOWATT_HOUR, timeSeries.getEnergyMeasurementUnitName());
        assertEquals(DirectionTypeList.DOWN, timeSeries.getFlowDirectionDirection());
        assertEquals(BusinessTypeList.CONSUMPTION, timeSeries.getBusinessType());
    }

    @Test
    void withEnergyData_withProductionMeterCode_setsMeteringInformation() throws InvalidMappingException {
        String meterCode = "1-1:2.9.0 P.01"; // Production meter
        EnergyData energyData = new SimpleEnergyData()
                .setMeterCode(meterCode)
                .setBillingUnit("MWH");

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withEnergyData(energyData);
        var timeSeries = uut.build();

        assertEquals(meterCode, timeSeries.getRegisteredResource().getMRID());
        assertEquals(UnitOfMeasureTypeList.MEGAWATT_HOURS, timeSeries.getEnergyMeasurementUnitName());
        assertEquals(DirectionTypeList.UP, timeSeries.getFlowDirectionDirection());
        assertEquals(BusinessTypeList.PRODUCTION, timeSeries.getBusinessType());
    }

    @Test
    void withEnergyData_withUnsupportedMeterCode_throwsInvalidMappingException() {
        String meterCode = "1-1:3.9.0 P.01";
        EnergyData energyData = new SimpleEnergyData()
                .setMeterCode(meterCode)
                .setBillingUnit("MWH");

        assertThrows(InvalidMappingException.class, () -> new TimeSeriesBuilder().withEnergyData(energyData));
    }

    @Test
    void withEnergyData_withUnsupportedUOMType_throwsInvalidMappingException() {
        String meterCode = "1-1:1.9.0 P.01";
        EnergyData energyData = new SimpleEnergyData()
                .setMeterCode(meterCode)
                .setBillingUnit("EUR");
        assertThrows(InvalidMappingException.class, () -> new TimeSeriesBuilder().withEnergyData(energyData));
    }

    @Test
    void withSeriesPeriod_setsSeriesPeriod() {
        SeriesPeriodComplexType seriesPeriod = new SeriesPeriodComplexType();

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withSeriesPeriod(seriesPeriod);
        var timeSeries = uut.build();

        assertEquals(seriesPeriod, timeSeries.getSeriesPeriodList().getSeriesPeriods().getFirst());
    }
}
