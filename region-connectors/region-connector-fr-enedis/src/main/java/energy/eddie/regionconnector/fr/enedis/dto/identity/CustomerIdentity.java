package energy.eddie.regionconnector.fr.enedis.dto.identity;

import com.fasterxml.jackson.annotation.JsonProperty;


public record CustomerIdentity(
        @JsonProperty("customer_id")
        String customerId,
        @JsonProperty("identity")
        Identity identity
) {
}
