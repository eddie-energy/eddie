package energy.eddie.regionconnector.fr.enedis.dto.address;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("usage_point")
@JsonTypeInfo(include = JsonTypeInfo.As.WRAPPER_OBJECT, use = JsonTypeInfo.Id.NAME)
public record UsagePoint(
        @JsonProperty("usage_point_id")
        String id,
        @JsonProperty("usage_point_status")
        String status,
        @JsonProperty("meter_type")
        String meterType,
        @JsonProperty("usage_point_addresses")
        Address address
) {
}
