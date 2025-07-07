package energy.eddie.regionconnector.fi.fingrid.client.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.regionconnector.fi.fingrid.client.deserializer.CustomerTransactionDeserializer;

public record CustomerData(@JsonProperty(value = "Header", required = true) Header header,
                           @JsonProperty(value = "Transaction", required = true)
                           @JsonDeserialize(using = CustomerTransactionDeserializer.class)
                           CustomerTransaction transaction) {
}
