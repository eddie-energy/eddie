package energy.eddie.regionconnector.shared.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ErrorResponse(
        @JsonProperty List<String> errors
) {
}