package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ErrorResponse(
        @NotNull
        @JsonProperty List<String> errors
) {
}