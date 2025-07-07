package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerPostalAddress(
        @JsonProperty("AddressNote") String addressNote,
        @JsonProperty("Apartment") String apartment,
        @JsonProperty("BuildingNumber") String buildingNumber,
        @JsonProperty(value = "CountryCode", required = true) String countryCode,
        @JsonProperty("POBox") String poBox,
        @JsonProperty(value = "PostalCode", required = true) String postalCode,
        @JsonProperty(value = "PostOffice", required = true) String postOffice,
        @JsonProperty("StairwellIdentification") String stairwellIdentification,
        @JsonProperty("StreetName") String streetName
) {
}
