package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CustomerDataResponse(
        @JsonProperty(value = "RetrieveCustomerResponse", required = true) CustomerData customerData) {
}
