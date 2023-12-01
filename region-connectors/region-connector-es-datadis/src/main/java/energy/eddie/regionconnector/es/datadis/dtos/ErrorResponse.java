package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ErrorResponse(
        @NotNull
        @JsonProperty List<String> errors
) {
}