package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.dto.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;
import static energy.eddie.regionconnector.shared.cim.v0_82.EsmpDateTime.ESMP_DATE_TIME_MINUTE_FORMATTER;
import static org.junit.jupiter.api.Assertions.*;

class SeriesPeriodBuilderTest {

    @Test
    void build_afterConstruction() {
        var seriesPeriod = new SeriesPeriodBuilder().build();
        assertNotNull(seriesPeriod);
        assertNull(seriesPeriod.getPointList());
    }

    @Test
    void withEnergyData_setsPointList_asExpected() throws InvalidMappingException {
        EnergyData energyData = new SimpleEnergyData()
                .setEnergyPositions(List.of(
                        new EnergyPosition(new BigDecimal(1), "L1"),
                        new EnergyPosition(new BigDecimal(2), "L2"),
                        new EnergyPosition(new BigDecimal(3), "L3")
                ));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder()
                .withEnergyData(energyData);

        var seriesPeriod = uut.build().getPointList();

        assertEquals(3, seriesPeriod.getPoints().size());
        assertEquals(QualityTypeList.AS_PROVIDED, seriesPeriod.getPoints().get(0).getEnergyQuantityQuality());
        assertEquals(QualityTypeList.ADJUSTED, seriesPeriod.getPoints().get(1).getEnergyQuantityQuality());
        assertEquals(QualityTypeList.ESTIMATED, seriesPeriod.getPoints().get(2).getEnergyQuantityQuality());
    }

    @Test
    void withEnergyData_withUnexpectedMeteringMethod_throwsInvalidMappingException() {
        EnergyData energyData = new SimpleEnergyData()
                .setEnergyPositions(List.of(
                        new EnergyPosition(new BigDecimal(1), "unexpected")
                ));

        assertThrows(InvalidMappingException.class, () -> new SeriesPeriodBuilder().withEnergyData(energyData));
    }

    @Test
    void build_withValidEnergy_withValidEnergyData_returnsExpected() throws InvalidMappingException {
        EnergyData energyData = new SimpleEnergyData()
                .setEnergyPositions(List.of(
                        new EnergyPosition(new BigDecimal(1), "L1")
                ));
        ZonedDateTime start = ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.MIN, AT_ZONE_ID);
        Energy energy = new SimpleEnergy()
                .setGranularity(Granularity.P1D)
                .setMeterReadingStart(start)
                .setMeterReadingEnd(start.plusDays(1));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy).withEnergyData(energyData);
        var seriesPeriod = uut.build();

        assertEquals(1, seriesPeriod.getPointList().getPoints().size());
    }

    @Test
    void withEnergy_withValidEnergy_setsTimeIntervalAsExpected() {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        ZonedDateTime start = ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.MIN, AT_ZONE_ID);
        ZonedDateTime end = start.plusDays(1);
        Energy energy = new SimpleEnergy()
                .setGranularity(Granularity.P1D)
                .setMeterReadingStart(start)
                .setMeterReadingEnd(end);

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        var timeInterval = uut.build().getTimeInterval();
        assertEquals(start.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                     LocalDateTime.parse(timeInterval.getStart(), ESMP_DATE_TIME_MINUTE_FORMATTER));
        assertEquals(end.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime(),
                     LocalDateTime.parse(timeInterval.getEnd(), ESMP_DATE_TIME_MINUTE_FORMATTER));
    }

    @ParameterizedTest
    @EnumSource(Granularity.class)
    void withEnergy_setsResolutionAsExpected(Granularity granularity) {
        ZonedDateTime start = ZonedDateTime.of(LocalDate.of(2021, 1, 1), LocalTime.MIN, AT_ZONE_ID);
        Energy energy = new SimpleEnergy()
                .setGranularity(granularity)
                .setMeterReadingStart(start)
                .setMeterReadingEnd(start.plusDays(1));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        assertEquals(granularity.name(), uut.build().getResolution());
    }
}
