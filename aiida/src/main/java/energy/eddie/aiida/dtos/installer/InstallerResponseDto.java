package energy.eddie.aiida.dtos.installer;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record InstallerResponseDto<T>(
        @JsonProperty("success") boolean success,
        @JsonProperty("error") @Nullable String error,
        @JsonProperty("data") @Nullable T data
) { }