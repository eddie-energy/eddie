package energy.eddie.regionconnector.es.datadis.dtos;

import jakarta.annotation.Nullable;

import java.util.Optional;

public record Address(
        @Nullable String street,
        @Nullable String buildingNumber,
        @Nullable String floor,
        @Nullable String door,
        @Nullable String postalCode,
        @Nullable String city,
        @Nullable String province
) {

    /**
     * Parsing an address string based on: "Street, Number , [FloorºDoor] PostalCode-City - Province" If the address
     * string does not match the expected format, an empty optional is returned.
     *
     * @param addressString a spanish address string
     * @return an optional address object
     */
    public static Optional<Address> parse(String addressString) {
        // Split the address into "Street"," Number ","[FloorºDoor] PostalCode-City - Province"
        var parts = addressString.split(",", 3);
        if (parts.length != 3) {
            return Optional.empty();
        }

        String street = parts[0].trim();
        String buildingNumber = parts[1].trim();

        // Split into "[FloorºDoor] PostalCode"-"City "-" Province"
        parts = parts[2].trim().split("-", 3);

        if (parts.length != 3) {
            return Optional.empty();
        }

        String municipality = parts[1].trim();
        String province = parts[2].trim();

        // Split into "[FloorºDoor]" "PostalCode"
        parts = parts[0].trim().split(" ", 2);
        // Initialize fields to null
        String postalCode;
        String floor = null;
        String door = null;
        if (parts.length == 1) {
            postalCode = parts[0].trim();
        } else {
            postalCode = parts[1].trim();
            // Split into "Floor" "Door"
            parts = parts[0].trim().split("º", 2);

            if (parts.length != 2) {
                door = parts[0].trim();
            } else {
                floor = parts[0].trim();
                door = parts[1].trim();
            }
        }

        return Optional.of(new Address(street, buildingNumber, floor, door, postalCode, municipality, province));
    }
}
