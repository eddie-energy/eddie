package energy.eddie.regionconnector.be.fluvius.client.model;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonProperty;

public enum ValidationState {
    @JsonProperty("EST")
    EST,
    @JsonProperty("READ")
    READ,
    @JsonEnumDefaultValue
    UNKNOWN
}
