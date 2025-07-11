package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

public record DataSourceSecretsDto(
        @Nullable @JsonProperty String plaintextPassword
) { }
