package energy.eddie.regionconnector.us.green.button.client.dtos.authorization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorizationNotes(@JsonProperty String type, @JsonProperty String msg, @JsonProperty ZonedDateTime ts) {
}
