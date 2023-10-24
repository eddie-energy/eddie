package energy.eddie.regionconnector.dk.energinet.enums;

import energy.eddie.regionconnector.dk.energinet.customer.model.Point;

import java.util.Objects;

/**
 * Enum for the Quality of the class Point
 * Documentation can be found at <a href="https://energinet.dk/media/xmdlhgel/customer-and-third-party-api-for-datahub-eloverblik-data-description.pdf">Energinet API Documentation</a> (last visited 12th of September 2023)
 *
 * @see energy.eddie.regionconnector.dk.energinet.customer.model.Point
 * @see Point#getOutQuantityQuality()
 */
public enum PointQualityEnum {
    A01("Adjusted"), A02("Not available"), A03("Estimated"), A04("As provided"), A05("Incomplete");
    private final String description;

    PointQualityEnum(String description) {
        this.description = description;
    }

    public static PointQualityEnum fromString(String code) {
        if (!Objects.requireNonNull(code).isBlank()) {
            for (PointQualityEnum enumValue : values()) {
                if (enumValue.name().equals(code)) {
                    return enumValue;
                }
            }
        }

        throw new IllegalArgumentException("Invalid PointQualityEnum value: " + code);
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return name() + "{" + "description='" + description + '\'' + '}';
    }
}
