package energy.eddie.regionconnector.fr.enedis.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UsagePointContract(
        @JsonProperty("usage_point")
        UsagePoint usagePoint,
        @JsonProperty("contracts")
        Contract contract
) {
}
