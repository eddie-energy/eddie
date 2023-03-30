package at.eda.xml.builders.customerprocesses.common.types._01p20;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProcessDirectorySBuilderTest {
    @Test
    public void testProcessDirectoryBuilder() {
        // Example of a correct implementation
        ProcessDirectorySBuilder processDirSBuilder = new ProcessDirectorySBuilder();
        processDirSBuilder
                .withMessageId("GC100007201912170930001230001234567")
                .withConversationId("GC100007201912170930001230012345678")
                .build();
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        ProcessDirectorySBuilder processDirSBuilder = new ProcessDirectorySBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> processDirSBuilder
                .withMessageId("thisIsARandomStringWithOver35Characters"));
        assertThrows(IllegalArgumentException.class, () -> processDirSBuilder
                .withConversationId("thisIsARandomStringWithOver35Characters"));
    }

    @Test
    public void testEmptyString() {
        // Assign empty string to required attributes
        ProcessDirectorySBuilder processDirSBuilder = new ProcessDirectorySBuilder();

        assertThrows(IllegalArgumentException.class, () -> processDirSBuilder
                .withMessageId(""));
        assertThrows(IllegalArgumentException.class, () -> processDirSBuilder
                .withConversationId(""));
    }

    @Test
    public void testIllegalStateException() {
        ProcessDirectorySBuilder processDirSBuilder = new ProcessDirectorySBuilder();

        // Assign no attributes
        assertThrows(IllegalStateException.class, processDirSBuilder::build);

        // Assign only one required attribute
        assertThrows(IllegalStateException.class, () -> processDirSBuilder
                .withMessageId("Test")
                .build());
    }
}
