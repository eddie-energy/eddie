package at.eda.xml.builders.customerprocesses.common.types._01p20;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class VerificationDocumentBuilderTest {
    @Test
    public void testVerificationDocumentBuilder() {
        // Example of a correct implementation
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        verificationDocumentBuilder
                .withDocNumber("Test")
                .build();
    }

    @Test
    public void testStringWrongCharacters() {
        // Assign string which contains not allowed characters
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("!§$&/(()="));
        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("SpecialCharacters!\"-.,"));
    }

    @Test
    public void testStringMaxLengthExceeded() {
        // Assign string which exceeds the maximum string length
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        // 35 Characters
        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder
                .withDocNumber("thisIsARandomStringWithOver35Characters"));
    }

    @Test
    public void testNullPointerException() {
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        // Assign no required attributes
        assertThrows(NullPointerException.class, verificationDocumentBuilder::build);
    }

    @Test
    public void testEmptyString() {
        VerificationDocumentBuilder verificationDocumentBuilder = new VerificationDocumentBuilder();

        assertThrows(IllegalArgumentException.class, () -> verificationDocumentBuilder.withDocNumber(""));
    }
}
