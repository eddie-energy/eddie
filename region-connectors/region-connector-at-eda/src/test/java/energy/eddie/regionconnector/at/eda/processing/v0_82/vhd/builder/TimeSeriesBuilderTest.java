package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.cim.validated_historical_data.v0_82.*;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
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
        assertEquals(EnergyProductTypeList.ACTIVE_POWER, timeSeries.getProduct());
    }

    @Test
    void withProcessDirectory_setsDateAndMarketEvaluationPoint() {
        String meteringPoint = "meteringPoint";
        XMLGregorianCalendar processDate = DateTimeConverter.dateToXml(LocalDate.of(2023, 1, 1));
        ProcessDirectory processDirectory = new ProcessDirectory()
                .withProcessDate(processDate)
                .withMeteringPoint(meteringPoint);

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withProcessDirectory(processDirectory);
        var timeSeries = uut.build();

        assertEquals(meteringPoint, timeSeries.getMarketEvaluationPointMRID().getValue());
        assertEquals(processDate, timeSeries.getRegistrationDateAndOrTimeDateTime().getDate());
    }

    @Test
    void withMarketParticipantDirectory_setsVersion() {
        String version = "version";
        MarketParticipantDirectory marketParticipantDirectory = new MarketParticipantDirectory()
                .withSchemaVersion(version);

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withMarketParticipantDirectory(marketParticipantDirectory);
        var timeSeries = uut.build();

        assertEquals(version, timeSeries.getVersion());
    }

    @Test
    void withEnergy_setsReason() {
        String meteringReason = "meteringReason";
        Energy energy = new Energy()
                .withMeteringReason(meteringReason);

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withEnergy(energy);
        var timeSeries = uut.build();

        assertEquals(meteringReason, timeSeries.getReasonList().getReasons().get(0).getText());
        assertEquals(ReasonCodeTypeList.ERRORS_NOT_SPECIFICALLY_IDENTIFIED, timeSeries.getReasonList().getReasons().get(0).getCode());
    }

    @Test
    void withEnergyData_withConsumptionMeterCode_setsMeteringInformation() throws InvalidMappingException {
        String meterCode = "1-1:1.9.0 P.01"; // Consumption meter
        EnergyData energyData = new EnergyData()
                .withMeterCode(meterCode)
                .withUOM(UOMType.KWH);

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
        EnergyData energyData = new EnergyData()
                .withMeterCode(meterCode)
                .withUOM(UOMType.MWH);

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
        EnergyData energyData = new EnergyData()
                .withMeterCode(meterCode)
                .withUOM(UOMType.MWH);

        assertThrows(InvalidMappingException.class, () -> new TimeSeriesBuilder().withEnergyData(energyData));
    }

    @Test
    void withEnergyData_withUnsupportedUOMType_throwsInvalidMappingException() {
        String meterCode = "1-1:1.9.0 P.01";
        EnergyData energyData = new EnergyData()
                .withMeterCode(meterCode) // Consumption meter
                .withUOM(UOMType.EUR);

        assertThrows(InvalidMappingException.class, () -> new TimeSeriesBuilder().withEnergyData(energyData));
    }

    @Test
    void withSeriesPeriod_setsSeriesPeriod() {
        SeriesPeriodComplexType seriesPeriod = new SeriesPeriodComplexType();

        TimeSeriesBuilder uut = new TimeSeriesBuilder().withSeriesPeriod(seriesPeriod);
        var timeSeries = uut.build();

        assertEquals(seriesPeriod, timeSeries.getSeriesPeriodList().getSeriesPeriods().get(0));
    }
}