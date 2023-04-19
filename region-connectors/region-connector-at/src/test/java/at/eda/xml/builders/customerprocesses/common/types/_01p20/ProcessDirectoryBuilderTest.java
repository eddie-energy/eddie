package at.eda.xml.builders.customerprocesses.common.types._01p20;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessDirectoryBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();
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
        at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder processDirBuilder = new at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder();

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
        at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder processDirBuilder = new at.eda.xml.builders.customerconsent.cmrequest._01p10.ProcessDirectoryBuilder();

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
    }

    @Test
    public void testNullPointerException() {
        ProcessDirectoryBuilder processDirBuilder = new ProcessDirectoryBuilder();

        // Assign no attributes
        assertThrows(NullPointerException.class, processDirBuilder::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withMessageId("Test")
                .build());

        // Assign only two required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withConversationId("Test")
                .build());

        // Assign only three required attribute
        assertThrows(NullPointerException.class, () -> processDirBuilder
                .withMeteringPoint("Test")
                .build());
    }
}
