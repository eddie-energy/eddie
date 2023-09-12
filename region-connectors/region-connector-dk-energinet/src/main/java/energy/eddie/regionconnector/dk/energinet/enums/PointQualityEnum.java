package energy.eddie.regionconnector.dk.energinet.enums;

import energy.eddie.regionconnector.dk.energinet.customer.model.Point;

/**
 * Enum for the Quality of the class Point
 * Documentation can be found at <a href="https://energinet.dk/media/xmdlhgel/customer-and-third-party-api-for-datahub-eloverblik-data-description.pdf">Energinet API Documentation</a> (last visited 12th of September 2023)
 *
 * @see energy.eddie.regionconnector.dk.energinet.customer.model.Point
 * @see Point#getOutQuantityQuality()
 */
public enum PointQualityEnum {
    A01("Adjusted", "Will no longer be used after February 2020. Until then it specifies energy quantities which are calculated by DataHub."), A02("Not available", "Specifies that the grid operator has submitted a 'missing indicator' to DataHub for the specific position, meaning that the energy quantity is not available. Therefore, no quantity will be returned for the specific position."), A03("Estimated", "Specifies that the grid operator has submitted the quantity to DataHub as an estimate."), A04("As provided", "Specifies that the grid operator has submitted the quantity to DataHub as measured."), A05("Incomplete", "Is applied to an aggregated energy quantity if at least one of the quantities included in the aggregation has been submitted to DataHub with a 'missing indicator', meaning that the quantity is not available (as described under code A02).");

    private final String description;
    private final String comment;

    PointQualityEnum(String description, String comment) {
        this.description = description;
        this.comment = comment;
    }

    public static PointQualityEnum fromString(String code) {
        for (PointQualityEnum enumValue : values()) {
            if (enumValue.name().equals(code)) {
                return enumValue;
            }
        }
        throw new IllegalArgumentException("Invalid PointQualityEnum value: " + code);
    }

    public String getDescription() {
        return description;
    }

    public String getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return name() + "{" + "description='" + description + '\'' + ", comment='" + comment + '\'' + '}';
    }
}
