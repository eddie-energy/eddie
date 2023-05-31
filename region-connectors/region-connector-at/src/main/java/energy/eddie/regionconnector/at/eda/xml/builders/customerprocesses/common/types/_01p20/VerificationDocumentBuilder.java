package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.VerificationDocument;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * <p>Allows to create a VerificationDocument Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see VerificationDocument
 */
public class VerificationDocumentBuilder {
    private static final int LEN_METERING_POINT = 35;
    @Nullable
    private String docNumber;

    /**
     * Sets the document id
     *
     * @param docNumber allowed object is
     *                  {@link String} max. length 35
     * @return {@link VerificationDocumentBuilder}
     */
    public VerificationDocumentBuilder withDocNumber(String docNumber) {
        if (Objects.requireNonNull(docNumber).isEmpty()) {
            throw new IllegalArgumentException("`docNumber` cannot be empty.");
        }

        if (docNumber.length() > LEN_METERING_POINT) {
            throw new IllegalArgumentException("`docNumber` length cannot exceed " + LEN_METERING_POINT + " characters.");
        }

        for (int i = 0; i < docNumber.length(); i++) {
            char ch = docNumber.charAt(i);
            if (!Character.isLetterOrDigit(ch)) {
                throw new IllegalArgumentException("`docNumber` must consist only of letters and digits.");
            }
        }

        this.docNumber = docNumber;
        return this;
    }

    /**
     * Creates and returns a VerificationDocument Object
     *
     * @return {@link VerificationDocument}
     */
    public VerificationDocument build() {
        VerificationDocument verificationDocument = new VerificationDocument();
        verificationDocument.setDOCNumber(Objects.requireNonNull(docNumber, "Attribute `docNumber` is required."));

        return verificationDocument;
    }
}
