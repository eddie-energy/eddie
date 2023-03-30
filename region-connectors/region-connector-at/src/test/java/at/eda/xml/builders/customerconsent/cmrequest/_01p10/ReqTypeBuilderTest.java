package at.eda.xml.builders.customerconsent.cmrequest._01p10;

import at.ebutilities.schemata.customerconsent.cmrequest._01p10.EnergyDirection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ReqTypeBuilderTest {

    @Test
    public void testReqTypeBuilder() {
        // Example of a correct implementation
        ReqTypeBuilder reqTypeBuilder = new ReqTypeBuilder();
        reqTypeBuilder
                .withReqDatType("EnergyCommunityRegistration")
                .withDateFrom(LocalDate.of(2022, Month.DECEMBER, 18))
                .withEcId("AT99999900000RC000000000012345678")
                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                .build();
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        ReqTypeBuilder reqTypeBuilder = new ReqTypeBuilder();

        // 30 characters
        assertThrows(IllegalArgumentException.class, () -> reqTypeBuilder
                .withReqDatType("thisIsARandomStringWithOver30Characters"));
        // 33 characters
        assertThrows(IllegalArgumentException.class, () -> reqTypeBuilder
                .withEcId("thisIsARandomStringWithOver33Characters"));
    }

    @Test
    public void testEmptyString() {
        // Assign empty string to required attribute
        ReqTypeBuilder reqTypeBuilder = new ReqTypeBuilder();
        assertThrows(IllegalArgumentException.class, () -> reqTypeBuilder
                .withReqDatType(""));
    }

    @Test
    public void testIllegalStateException() {
        // Assign no attributes
        ReqTypeBuilder reqTypeBuilder1 = new ReqTypeBuilder();
        assertThrows(IllegalStateException.class, reqTypeBuilder1::build);

        // Assign only one required attribute
        assertThrows(IllegalStateException.class, () -> reqTypeBuilder1
                .withReqDatType("Test")
                .build());
        ReqTypeBuilder reqTypeBuilder2 = new ReqTypeBuilder();
        assertThrows(IllegalStateException.class, () -> reqTypeBuilder2
                .withDateFrom(LocalDate.of(2023, Month.FEBRUARY, 6))
                .build());

        // Assign toDate which is before fromDate
        assertThrows(IllegalStateException.class, () -> reqTypeBuilder1
                .withDateFrom(LocalDate.of(2023, Month.MARCH, 6))
                .withDateTo(LocalDate.of(2023, Month.FEBRUARY, 6))
                .build());
    }
}
