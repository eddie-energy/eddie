package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;

public record EnedisContractApiResponse(
        @JsonProperty("customer")
        CustomerContract customer
) {
}
