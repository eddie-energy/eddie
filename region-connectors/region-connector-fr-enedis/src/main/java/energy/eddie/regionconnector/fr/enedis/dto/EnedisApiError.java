package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EnedisApiError(@JsonProperty String error, @JsonProperty("error_message") String errorMessage) {
}
