package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdditionalDataBuilderTest {
    @Test
    void testAdditionalDataBuilder() {
        // Example of a correct implementation
        AdditionalDataBuilder additionalDataBuilder = new AdditionalDataBuilder();

        assertDoesNotThrow(
                additionalDataBuilder
                .withValue("Test")
                .withName("Test")
                        ::build
        );
    }

    @Test
    void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        AdditionalDataBuilder additionalDataBuilder = new AdditionalDataBuilder();

        // 40 Characters
        assertThrows(IllegalArgumentException.class, () -> additionalDataBuilder
                .withName("thisIsARandomStringWithOver40Charactersxxx"));
        // 120 Characters
        assertThrows(IllegalArgumentException.class, () -> additionalDataBuilder
                .withValue(
                        "thisIsARandomStringWithOver120Charactersxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                                "xxxxxxxxxxxxxxxxxxxxxxxxxxx"
                ));
    }

    @Test
    void testEmptyString() {
        AdditionalDataBuilder additionalDataBuilder = new AdditionalDataBuilder();

        assertThrows(IllegalArgumentException.class, () -> additionalDataBuilder.withName(""));
        assertThrows(IllegalArgumentException.class, () -> additionalDataBuilder.withValue(""));
    }

    @Test
    void testNullPointerException() {
        AdditionalDataBuilder additionalDataBuilder = new AdditionalDataBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, additionalDataBuilder::build);

        // Assign only one required attribute
        assertThrows(NullPointerException.class, additionalDataBuilder.withValue("Test")::build);
    }
}
