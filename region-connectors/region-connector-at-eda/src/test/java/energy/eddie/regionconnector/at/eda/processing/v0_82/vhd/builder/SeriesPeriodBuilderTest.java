package energy.eddie.regionconnector.at.eda.processing.v0_82.vhd.builder;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.Energy;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyData;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyPosition;
import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.MeteringIntervall;
import energy.eddie.cim.v0_82.vhd.QualityTypeList;
import energy.eddie.regionconnector.at.eda.InvalidMappingException;
import energy.eddie.regionconnector.at.eda.xml.helper.DateTimeConverter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.EsmpDateTime.ESMP_DATE_TIME_FORMATTER;
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
        EnergyData energyData = new EnergyData()
                .withEP(List.of(
                        new EnergyPosition().withBQ(new BigDecimal(1)).withMM("L1"),
                        new EnergyPosition().withBQ(new BigDecimal(2)).withMM("L2"),
                        new EnergyPosition().withBQ(new BigDecimal(3)).withMM("L3")
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
        EnergyData energyData = new EnergyData()
                .withEP(List.of(
                        new EnergyPosition().withBQ(new BigDecimal(1)).withMM("unexpected")
                ));

        assertThrows(InvalidMappingException.class, () -> new SeriesPeriodBuilder().withEnergyData(energyData));
    }

    @Test
    void build_withValidEnergy_withValidEnergyData_returnsExpected() throws InvalidMappingException {
        EnergyData energyData = new EnergyData()
                .withEP(List.of(
                        new EnergyPosition().withBQ(new BigDecimal(1)).withMM("L1")
                ));
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.D)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(start.plusDays(1)));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy).withEnergyData(energyData);
        var seriesPeriod = uut.build();

        assertEquals(1, seriesPeriod.getPointList().getPoints().size());
    }

    @Test
    void withEnergy_withValidEnergy_setsTimeIntervalAsExpected() throws InvalidMappingException {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        LocalDateTime end = start.plusDays(1);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.D)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(end));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        var timeInterval = uut.build().getTimeInterval();
        assertEquals(start, LocalDateTime.parse(timeInterval.getStart(), ESMP_DATE_TIME_FORMATTER));
        assertEquals(end, LocalDateTime.parse(timeInterval.getEnd(), ESMP_DATE_TIME_FORMATTER));
    }

    @Test
    void withEnergy_withDailyMeteringInterval_setsResolutionAsExpected() throws InvalidMappingException {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.D)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(start.plusDays(1)));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        Duration resolution = Duration.parse(uut.build().getResolution());
        assertEquals(Duration.ofDays(1), resolution);
    }

    @Test
    void withEnergy_withQuarterHourlyMeteringInterval_setsResolutionAsExpected() throws InvalidMappingException {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.QH)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(start.plusDays(1)));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        Duration resolution = Duration.parse(uut.build().getResolution());
        assertEquals(Duration.ofMinutes(15), resolution);
    }

    @Test
    void withEnergy_withHourlyMeteringInterval_setsResolutionAsExpected() throws InvalidMappingException {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.H)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(start.plusDays(1)));

        SeriesPeriodBuilder uut = new SeriesPeriodBuilder().withEnergy(energy);

        Duration resolution = Duration.parse(uut.build().getResolution());
        assertEquals(Duration.ofHours(1), resolution);
    }

    @Test
    void withEnergy_withVariableMeteringInterval_throwsInvalidMappingException() {
        // following this document https://www.ebutilities.at/documents/20220309103941_datentypen.pdf
        LocalDateTime start = LocalDateTime.of(2021, 1, 1, 0, 0, 0);
        Energy energy = new Energy()
                .withMeteringIntervall(MeteringIntervall.V)
                .withMeteringPeriodStart(DateTimeConverter.dateTimeToXml(start))
                .withMeteringPeriodEnd(DateTimeConverter.dateTimeToXml(start.plusDays(1)));

        assertThrows(InvalidMappingException.class, () -> new SeriesPeriodBuilder().withEnergy(energy));
    }
}