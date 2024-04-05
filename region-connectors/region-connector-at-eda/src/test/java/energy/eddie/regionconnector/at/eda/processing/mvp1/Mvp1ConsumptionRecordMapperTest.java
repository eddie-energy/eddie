package energy.eddie.regionconnector.at.eda.processing.mvp1;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.ConsumptionPoint;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.*;
import energy.eddie.regionconnector.at.eda.utils.ConversionFactor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static org.junit.jupiter.api.Assertions.*;

class Mvp1ConsumptionRecordMapperTest {

    private static Stream<Arguments> consumptionRecordConfigurations() {
        return Stream.of(
                Arguments.of("L1", 10, Granularity.PT15M, "KWH"),
                Arguments.of("L2", 1, Granularity.P1D, "MWH"),
                Arguments.of("L3", 2, Granularity.P1D, "GWH"),
                Arguments.of("L2", 0.05, Granularity.PT15M, "KWH"),
                Arguments.of("L1", 0.7, Granularity.PT15M, "MWH")
        );
    }

    @ParameterizedTest
    @MethodSource("consumptionRecordConfigurations")
    void mapToMvp1ConsumptionRecord_mapsEDAConsumptionRecordWithSingleConsumptionPoint_asExpected(
            String meteringType,
            double consumptionValue,
            Granularity granularity,
            String unit
    ) throws InvalidMappingException {
        var meteringPoint = "meteringPoint";
        var expectedMeteringType = meteringType.equals("L1") ? ConsumptionPoint.MeteringType.MEASURED_VALUE : ConsumptionPoint.MeteringType.EXTRAPOLATED_VALUE;
        var conversionFactor = switch (unit) {
            case "KWH" -> ConversionFactor.KWH_TO_WH;
            case "MWH" -> ConversionFactor.MWH_TO_WH;
            case "GWH" -> ConversionFactor.GWH_TO_WH;
            default -> throw new IllegalArgumentException("Unexpected value: " + unit);
        };
        var expectedWh = consumptionValue * conversionFactor.getFactor();

        var edaCR = createConsumptionRecord(meteringPoint,
                                            meteringType,
                                            ZonedDateTime.now(ZoneOffset.UTC),
                                            granularity,
                                            consumptionValue,
                                            unit);

        var uut = new Mvp1ConsumptionRecordMapper();

        var cimCR = uut.mapToMvp1ConsumptionRecord(edaCR);

        assertEquals(meteringPoint, cimCR.getMeteringPoint());
        assertNotNull(cimCR.getConsumptionPoints());
        assertEquals(granularity.name(), cimCR.getMeteringInterval());
        assertEquals(1, cimCR.getConsumptionPoints().size());
        assertEquals(expectedMeteringType, cimCR.getConsumptionPoints().getFirst().getMeteringType());
        assertEquals(expectedWh, cimCR.getConsumptionPoints().getFirst().getConsumption());
    }

    private EdaConsumptionRecord createConsumptionRecord(
            String meteringPoint,
            String meteringType,
            ZonedDateTime meteringPeriodStart,
            Granularity granularity,
            double consumptionValue,
            String unit
    ) {
        return new SimpleEdaConsumptionRecord()
                .setStartDate(meteringPeriodStart.toLocalDate())
                .setMeteringPoint(meteringPoint)
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setMeterReadingStart(meteringPeriodStart)
                                .setGranularity(granularity)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setBillingUnit(unit)
                                                .setEnergyPositions(List.of(
                                                        new EnergyPosition(BigDecimal.valueOf(
                                                                consumptionValue), meteringType))
                                                ))
                                )
                ));
    }

    @Test
    void mapToMvp1ConsumptionRecord_EdaConsumptionRecordInvalidBillingUnit_throwsInvalidMappingException() {
        var edaCR = new SimpleEdaConsumptionRecord()
                .setStartDate(LocalDate.now(AT_ZONE_ID))
                .setEnergy(List.of(
                        new SimpleEnergy()
                                .setGranularity(Granularity.PT15M)
                                .setEnergyData(List.of(
                                        new SimpleEnergyData()
                                                .setBillingUnit("XXX")
                                ))
                ));
        var uut = new Mvp1ConsumptionRecordMapper();

        assertThrows(InvalidMappingException.class, () -> uut.mapToMvp1ConsumptionRecord(edaCR));
    }

    @Test
    void mapToMvp1ConsumptionRecord_EdaConsumptionRecordIsNull_throwsNullPointerException() {
        var uut = new Mvp1ConsumptionRecordMapper();

        assertThrows(NullPointerException.class, () -> uut.mapToMvp1ConsumptionRecord(null));
    }

    @Test
    void mapToMvp1ConsumptionRecord_mapsEDAConsumptionRecordWithSingleConsumptionPoint_returnsAtTimeZoneInformation() throws InvalidMappingException {
        var austrianTime = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, AT_ZONE_ID);
        var edaCR = createConsumptionRecord("xxx", "L1", austrianTime, Granularity.PT15M, 10, "KWH");

        var uut = new Mvp1ConsumptionRecordMapper();

        var cimCR = uut.mapToMvp1ConsumptionRecord(edaCR);

        assertNotNull(cimCR.getConsumptionPoints());
        assertEquals(1, cimCR.getConsumptionPoints().size());
        assertEquals(austrianTime, cimCR.getStartDateTime());
        assertEquals(AT_ZONE_ID, cimCR.getStartDateTime().getZone());
    }
}
