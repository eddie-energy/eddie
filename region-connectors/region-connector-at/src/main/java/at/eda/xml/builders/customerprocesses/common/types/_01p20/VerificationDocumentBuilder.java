package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.VerificationDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Allows to create a VerificationDocument Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see VerificationDocument
 */
public class VerificationDocumentBuilder {
    private String docNumber = "";

    /**
     * Sets the document id
     *
     * @param docNumber allowed object is
     *                  {@link String} max. length 35
     * @return {@link VerificationDocumentBuilder}
     */
    public VerificationDocumentBuilder withDocNumber(String docNumber) {
        if (docNumber == null || docNumber.length() == 0) {
            throw new IllegalArgumentException("`docNumber` cannot be empty.");
        }

        int LEN_METERING_POINT = 35;
        if (docNumber.length() > LEN_METERING_POINT) {
            throw new IllegalArgumentException("`docNumber` length cannot exceed " + LEN_METERING_POINT + " characters.");
        }

        String regex = "[0-9A-Za-z]*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(docNumber);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("`docNumber` does not match the necessary pattern (" + regex + ").");
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
        if (docNumber.length() == 0) {
            throw new IllegalStateException("Attribute `docNumber` is required.");
        }

        VerificationDocument verificationDocument = new VerificationDocument();
        verificationDocument.setDOCNumber(docNumber);

        return verificationDocument;
    }
}
