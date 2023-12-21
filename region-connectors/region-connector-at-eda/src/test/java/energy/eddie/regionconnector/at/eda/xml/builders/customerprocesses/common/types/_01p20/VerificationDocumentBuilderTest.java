package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerificationDocumentBuilderTest {
    @Test
    void testVerificationDocumentBuilder() {
        // Example of a correct implementation
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder()
                .withDocNumber("Test");
        assertDoesNotThrow(verificationDocumentBuilder::build);
    }

    @Test
    void testStringWrongCharacters() {
        // Assign string which contains not allowed characters
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("!§$&/(()="));
        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("SpecialCharacters!\"-.,"));
    }

    @Test
    void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("thisIsARandomStringWithOver35Characters"));
    }

    @Test
    void testNullPointerException() {
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, verificationDocumentBuilder::build);
    }

    @Test
    void testEmptyString() {
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder.withDocNumber(""));
    }
}
