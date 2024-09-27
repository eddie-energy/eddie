package energy.eddie.regionconnector.us.green.button.client.dtos.authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Scope(@JsonProperty String expires, @JsonProperty("ongoing_end") @Nullable ZonedDateTime ongoingEnd) {
}
