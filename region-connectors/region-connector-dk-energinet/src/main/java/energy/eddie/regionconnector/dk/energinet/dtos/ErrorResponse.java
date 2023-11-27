package energy.eddie.regionconnector.dk.energinet.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record ErrorResponse(
        @NotNull
        @JsonProperty List<String> errors
) {
}