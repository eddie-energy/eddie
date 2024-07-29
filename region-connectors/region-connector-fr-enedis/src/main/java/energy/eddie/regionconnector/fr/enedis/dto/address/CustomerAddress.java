package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.List;

// this makes it possible to unwrap the customer object without an additional wrapper object
@JsonTypeName("customer")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public record CustomerAddress(
        @JsonProperty("customer_id")
        String customerId,
        @JsonProperty("usage_points")
        List<UsagePoint> usagePoints
) {
}
