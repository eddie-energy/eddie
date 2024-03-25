package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ErrorResponse(
        @Schema(description = "List of errors that occurred during the request.", example = "startTime must not be null.")
        @NotNull
        @JsonProperty List<String> errors
) {
}
