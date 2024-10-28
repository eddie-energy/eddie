package energy.eddie.regionconnector.us.green.button.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.annotation.Nullable;

import java.net.URI;
import java.time.ZonedDateTime;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookEvent(
        @JsonProperty(required = true)
        String uid,
        @JsonProperty(required = true)
        String type,
        @JsonProperty(required = true)
        ZonedDateTime ts,
        @JsonProperty(required = true)
        String deliveryMethod,
        @JsonProperty(required = true)
        URI deliveryTarget,
        @JsonProperty(required = true)
        boolean isDelivered,
        @Nullable
        String authorizationUid,
        @Nullable String meterUid
) {
}
