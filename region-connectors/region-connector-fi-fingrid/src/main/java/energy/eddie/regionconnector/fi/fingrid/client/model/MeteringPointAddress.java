package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MeteringPointAddress(
        @JsonProperty("AddressNote") String addressNote,
        @JsonProperty("Apartment") String apartment,
        @JsonProperty("BuildingNumber") String buildingNumber,
        @JsonProperty(value = "CountryCode", required = true) String countryCode,
        @JsonProperty(value = "Language", required = true) String language,
        @JsonProperty(value = "PostalCode", required = true) String postalCode,
        @JsonProperty(value = "PostOffice", required = true) String postOffice,
        @JsonProperty("StairwellIdentification") String stairwellIdentification,
        @JsonProperty("StreetName") String streetName
) {
}
