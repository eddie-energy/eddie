package energy.eddie.regionconnector.at.eda.xml.builders.customerprocesses.common.types._01p20;

import at.ebutilities.schemata.customerprocesses.common.types._01p20.AdditionalData;

import jakarta.annotation.Nullable;
import java.util.Objects;

/**
 * <p>Allows to create a AdditionalData Object (Common Type).
 * <p>All fields are required according to the schema definition.
 *
 * @see AdditionalData
 */
public class AdditionalDataBuilder {
    private static final int LEN_VALUE = 120;
    private static final int LEN_NAME = 40;
    @Nullable
    private String value;
    @Nullable
    private String name;

    /**
     * Sets additional information
     *
     * @param value allowed object is
     *              {@link String}
     * @return {@link AdditionalDataBuilder}
     */
    public AdditionalDataBuilder withValue(String value) {
        if (Objects.requireNonNull(value).isEmpty()) {
            throw new IllegalArgumentException("`value` cannot be empty.");
        }

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
        if (Objects.requireNonNull(name).isEmpty()) {
            throw new IllegalArgumentException("`name` cannot be empty.");
        }

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
        AdditionalData additionalData = new AdditionalData();
        additionalData.setValue(Objects.requireNonNull(value, "Attribute `value` is required."));
        additionalData.setName(Objects.requireNonNull(name, "Attribute `name` is required."));

        return additionalData;
    }
}
