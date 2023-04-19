package at.eda.xml.builders.customerconsent.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.MeteringIntervallType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ParamHistTypeBuilderTest {
    @Test
    public void testParamHistTypeBuilder() {
        // Example of a correct implementation
        ParamHistTypeBuilder paramHistTypeBuilder = new ParamHistTypeBuilder();

        LocalDate dateFrom = LocalDate.of(2023, Month.FEBRUARY, 6);
        LocalDate dateTo = LocalDate.of(2023, Month.FEBRUARY, 7);
        paramHistTypeBuilder
                .withDateFrom(dateTo)
                .withDateTo(dateFrom)
                .withMeteringIntervall(MeteringIntervallType.D);
    }

    @Test
    public void testNullPointerException() {
        ParamHistTypeBuilder paramHistTypeBuilder1 = new ParamHistTypeBuilder();
        ParamHistTypeBuilder paramHistTypeBuilder2 = new ParamHistTypeBuilder();
        ParamHistTypeBuilder paramHistTypeBuilder3 = new ParamHistTypeBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, paramHistTypeBuilder1::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> paramHistTypeBuilder1.withDateFrom(
                LocalDate.of(2023, Month.FEBRUARY, 6)).build());

        // Assign only two required attributes
        assertThrows(NullPointerException.class, () -> paramHistTypeBuilder1.withDateTo(
                LocalDate.of(2023, Month.FEBRUARY, 7)).build());
        assertThrows(NullPointerException.class, () -> paramHistTypeBuilder2
                .withDateFrom(LocalDate.of(2023, Month.FEBRUARY, 6))
                .withMeteringIntervall(MeteringIntervallType.D).build());

        // fromDate is after toDate
        paramHistTypeBuilder3
                .withDateFrom(LocalDate.of(2023, Month.FEBRUARY, 7))
                .withDateTo(LocalDate.of(2023, Month.FEBRUARY, 6))
                .withMeteringIntervall(MeteringIntervallType.D);
    }
}
