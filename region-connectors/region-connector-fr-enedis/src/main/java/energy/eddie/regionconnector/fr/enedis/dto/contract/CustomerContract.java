package energy.eddie.regionconnector.fr.enedis.dto.contract;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CustomerContract(
        @JsonProperty("customer_id")
        String customerId,
        @JsonProperty("usage_points")
        List<UsagePointContract> usagePointContracts
) {
}
