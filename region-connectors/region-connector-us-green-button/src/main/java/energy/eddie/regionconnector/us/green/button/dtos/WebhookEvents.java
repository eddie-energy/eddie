package energy.eddie.regionconnector.us.green.button.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.annotation.Nullable;

import java.util.List;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookEvents(@JsonProperty(required = true) List<WebhookEvent> events,
                            @Nullable String next) {
}
