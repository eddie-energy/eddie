package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginDto(
        @JsonProperty("userId") String userId,
        @JsonProperty("installerToken") String installerToken
) { }