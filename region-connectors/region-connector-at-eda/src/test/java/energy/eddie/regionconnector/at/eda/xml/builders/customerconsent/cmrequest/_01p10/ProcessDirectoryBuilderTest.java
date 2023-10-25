package energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10;


import at.ebutilities.schemata.customerconsent.cmrequest._01p10.EnergyDirection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessDirectoryBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();
        ReqTypeBuilder reqTypeBuilder = new ReqTypeBuilder();

        processDirBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .withConversationId("GC100007201912170930001230012345678")
                .withProcessDate(LocalDate.of(2019, Month.DECEMBER, 17))
                .withMeteringPoint("AT9999990699900000000000206868100")
                .withCMRequestId("IWRN74PW")
                .withConsentId("AT999999201912171011121230023456789")
                .withCMRequest(
                        reqTypeBuilder
                                .withReqDatType("EnergyCommunityRegistration")
                                .withDateFrom(LocalDate.of(2022, Month.DECEMBER, 18))
                                .withEcId("AT99999900000RC000000000012345678")
                                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                                .build()
                )
                .build();

        ProcessDirectoryBuilder processDirBuilderWithDefaults = new ProcessDirectoryBuilder();

        processDirBuilderWithDefaults
                .withMessageId("GC100007201912170930001230001234567")
                .withConversationId("GC100007201912170930001230012345678")
                .withMeteringPoint("AT9999990699900000000000206868100")
                .withConsentId("AT999999201912171011121230023456789")
                .withCMRequest(
                        reqTypeBuilder
                                .withReqDatType("EnergyCommunityRegistration")
                                .withEcId("AT99999900000RC000000000012345678")
                                .withEnergyDirection(EnergyDirection.CONSUMPTION)
                                .build()
                )
                .build();
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMessageId("thisIsARandomStringWithOver35Characters"));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConversationId("thisIsARandomStringWithOver35Characters"));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withCMRequestId("thisIsARandomStringWithOver35Characters"));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConsentId("thisIsARandomStringWithOver35Characters"));

        // 33 characters
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint("thisIsARandomStringWithOver33Characters"));
    }

    @Test
    public void testStringWrongCharacters() {
        // Assign string which contains not allowed characters
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint("!§$&/(()="));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint("SpecialCharacters!\"-.,"));
    }

    @Test
    public void testEmptyString() {
        // Assign empty string to required attributes
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMessageId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConversationId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConsentId(""));
    }

    @Test
    public void testNullPointerException() {
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        // Assign no attributes
        assertThrows(NullPointerException.class, processDirBuilder::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .build());

    }
}
