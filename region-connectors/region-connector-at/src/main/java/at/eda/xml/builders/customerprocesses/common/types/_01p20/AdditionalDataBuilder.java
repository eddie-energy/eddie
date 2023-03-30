package at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AdditionalData;

/**
 * <p>Allows to create a AdditionalData Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see AdditionalData
 */
public class AdditionalDataBuilder {
    private String value = "";
    private String name = "";

    /**
     * Sets additional information
     *
     * @param value allowed object is
     *              {@link String}
     * @return {@link AdditionalDataBuilder}
     */
    public AdditionalDataBuilder withValue(String value) {
        if (value == null || value.length() == 0) {
            throw new IllegalArgumentException("`value` cannot be empty.");
        }

        int LEN_VALUE = 120;
        if (value.length() > LEN_VALUE) {
            throw new IllegalArgumentException("`value` length cannot exceed " + LEN_VALUE + " characters.");
        }

        this.value = value;
        return this;
    }

    /**
     * Sets the coding (description) of the info
     *
     * @param name allowed object is
     *             {@link String}
     * @return {@link AdditionalDataBuilder}
     */
    public AdditionalDataBuilder withName(String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("`name` cannot be empty.");
        }

        int LEN_NAME = 40;
        if (name.length() > LEN_NAME) {
            throw new IllegalArgumentException("`name` length cannot exceed " + LEN_NAME + " characters.");
        }

        this.name = name;
        return this;
    }

    /**
     * Creates and returns a AdditionalData Object
     *
     * @return {@link AdditionalData}
     */
    public AdditionalData build() {
        if (value.length() == 0 || name.length() == 0) {
            throw new IllegalStateException("Attributes `name` and `value` are required.");
        }

        AdditionalData additionalData = new AdditionalData();
        additionalData.setValue(value);
        additionalData.setName(name);

        return additionalData;
    }
}
