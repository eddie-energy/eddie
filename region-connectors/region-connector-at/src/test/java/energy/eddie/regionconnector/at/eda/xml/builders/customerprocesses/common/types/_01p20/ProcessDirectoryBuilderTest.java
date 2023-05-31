package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import energy.eddie.regionconnector.at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessDirectoryBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder processDirBuilder = new energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder();
        processDirBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .withConversationId("GC100007201912170930001230012345678")
                .withProcessDate(LocalDate.of(2019, Month.DECEMBER, 17))
                .withMeteringPoint("AT9999990699900000000000206868100")
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
        energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder processDirBuilder = new energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint("!§$&/(()="));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint("SpecialCharacters!\"-.,"));
    }

    @Test
    public void testEmptyString() {
        // Assign empty string to required attributes
        energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder processDirBuilder = new energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMessageId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withConversationId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirBuilder
                .withMeteringPoint(""));
    }

    @Test
    public void testNullPointerException() {
        energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder processDirBuilder = new energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20.ProcessDirectoryBuilder();

        // Assign no attributes
        assertThrows(NullPointerException.class, processDirBuilder::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .build());

        // Assign only two required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withMeteringPoint("AT9999990699900000000000206868100")
                .build());
    }
}
