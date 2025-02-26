package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record LoginDto(
        @JsonProperty("userId") UUID userId,
        @JsonProperty("installerToken") String installerToken
) { }