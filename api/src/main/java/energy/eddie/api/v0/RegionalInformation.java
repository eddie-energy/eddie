package energy.eddie.api.v0;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = RegionalInformation.class)
@JsonPropertyOrder(alphabetic = true)
public interface RegionalInformation {

    @JsonProperty
    String countryCode();

    @JsonProperty
    String regionConnectorId();

    @JsonProperty
    String meteringDataAdministratorId();

    @JsonProperty
    String permissionAdministratorId();
}