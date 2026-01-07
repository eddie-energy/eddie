package energy.eddie.regionconnector.us.green.button.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookEvents(@JsonProperty(required = true) List<WebhookEvent> events,
                            @Nullable String next) {
}
