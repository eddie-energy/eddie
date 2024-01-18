package energy.eddie.regionconnector.at.eda;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.*;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.regionconnector.at.eda.utils.ConversionFactor;
import energy.eddie.regionconnector.at.eda.utils.DateTimeConstants;
import energy.eddie.regionconnector.at.eda.utils.MeteringIntervalUtil;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ConsumptionRecordMapperTest {

    private static Stream<Arguments> consumptionRecordConfigurations() {
        return Stream.of(
                Arguments.of("L1", 10, MeteringIntervall.QH, UOMType.KWH),
                Arguments.of("L2", 1, MeteringIntervall.D, UOMType.MWH),
                Arguments.of("L3", 2, MeteringIntervall.D, UOMType.GWH),
                Arguments.of("L2", 0.05, MeteringIntervall.QH, UOMType.KWH),
                Arguments.of("L1", 0.7, MeteringIntervall.QH, UOMType.MWH),
                Arguments.of("L3", 0.0001, MeteringIntervall.H, UOMType.GWH),
                Arguments.of("L1", 100, MeteringIntervall.H, UOMType.KWH),
                Arguments.of("L2", 1000, MeteringIntervall.H, UOMType.MWH)
        );
    }

    @ParameterizedTest
    @MethodSource("consumptionRecordConfigurations")
    void mapToCIM_mapsEDAConsumptionRecordWithSingleConsumptionPoint_asExpected(String meteringType, double consumptionValue, MeteringIntervall meteringInterval, UOMType unit) throws InvalidMappingException {
        var meteringPoint = "meteringPoint";
        var expectedMeteringType = meteringType.equals("L1") ? ConsumptionPoint.MeteringType.MEASURED_VALUE : ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
        var conversionFactor = switch (unit) {
            case KWH -> ConversionFactor.KWH_TO_WH;
            case MWH -> ConversionFactor.MWH_TO_WH;
            case GWH -> ConversionFactor.GWH_TO_WH;
            default -> throw new IllegalArgumentException("Unexpected value: " + unit);
        };
        var expectedMeteringInterval = MeteringIntervalUtil.toGranularity(meteringInterval);
        var expectedWh = consumptionValue * conversionFactor.getFactor();

        var edaCR = createConsumptionRecord(meteringPoint, meteringType, ZonedDateTime.now(ZoneOffset.UTC), meteringInterval, consumptionValue, unit);

        var uut = new ConsumptionRecordMapper();

        var cimCR = uut.mapToCIM(edaCR);

        assertEquals(meteringPoint, cimCR.getMeteringPoint());
        assertNotNull(cimCR.getConsumptionPoints());
        assertEquals(expectedMeteringInterval.name(), cimCR.getMeteringInterval());
        assertEquals(1, cimCR.getConsumptionPoints().size());
        assertEquals(expectedMeteringType, cimCR.getConsumptionPoints().getFirst().getMeteringType());
        assertEquals(expectedWh, cimCR.getConsumptionPoints().getFirst().getConsumption());
    }

    @Test
    void mapToCIM_EdaConsumptionRecordWithEmptyProcessDirectory_throwsInvalidMappingException() {
        var edaCR = new ConsumptionRecord();
        edaCR.setProcessDirectory(new ProcessDirectory());
        var uut = new ConsumptionRecordMapper();

        assertThrows(InvalidMappingException.class, () -> uut.mapToCIM(edaCR));
    }

    @Test
    void mapToCIM_EdaConsumptionRecordWithEmptyEnergy_throwsInvalidMappingException() {
        var edaCR = createConsumptionRecord("test", "L1", ZonedDateTime.now(ZoneOffset.UTC), MeteringIntervall.QH, 1, UOMType.KWH);
        edaCR.getProcessDirectory().getEnergy().forEach(e -> e.getEnergyData().clear());
        var uut = new ConsumptionRecordMapper();

        assertThrows(InvalidMappingException.class, () -> uut.mapToCIM(edaCR));
    }

    @Test
    void mapToCIM_EdaConsumptionRecordIsNull_throwsNullPointerException() {
        var uut = new ConsumptionRecordMapper();

        assertThrows(NullPointerException.class, () -> uut.mapToCIM(null));
    }

    @Test
    void mapToCIM_mapsEDAConsumptionRecordWithSingleConsumptionPoint_returnsAtTimeZoneInformation() throws InvalidMappingException {
        var austrianTime = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, DateTimeConstants.AT_ZONE_ID);
        var edaCR = createConsumptionRecord("xxx", "L1", austrianTime, MeteringIntervall.QH, 10, UOMType.KWH);

        var uut = new ConsumptionRecordMapper();

        var cimCR = uut.mapToCIM(edaCR);

        assertNotNull(cimCR.getConsumptionPoints());
        assertEquals(1, cimCR.getConsumptionPoints().size());
        assertEquals(austrianTime, cimCR.getStartDateTime());
        assertEquals(DateTimeConstants.AT_ZONE_ID, cimCR.getStartDateTime().getZone());
    }


    private ConsumptionRecord createConsumptionRecord(String meteringPoint, String meteringType, ZonedDateTime meteringPeriodStart, MeteringIntervall meteringIntervall, double consumptionValue, UOMType unit) {
        var edaCR = new ConsumptionRecord();
        ProcessDirectory processDirectory = new ProcessDirectory();
        edaCR.setProcessDirectory(processDirectory);
        processDirectory.setMeteringPoint(meteringPoint);
        Energy energy = new Energy();
        energy.setMeteringIntervall(meteringIntervall);
        energy.setNumberOfMeteringIntervall(BigInteger.valueOf(1));
        energy.setMeteringPeriodStart(DateTimeConverter.dateToXml(meteringPeriodStart.toLocalDate()));
        EnergyData energyData = new EnergyData();
        energyData.setUOM(unit);
        EnergyPosition energyPosition = new EnergyPosition();
        energyPosition.setMM(meteringType);
        energyPosition.setBQ(BigDecimal.valueOf(consumptionValue));
        energyData.getEP().add(energyPosition);
        energy.getEnergyData().add(energyData);
        processDirectory.getEnergy().add(energy);
        return edaCR;
    }
}