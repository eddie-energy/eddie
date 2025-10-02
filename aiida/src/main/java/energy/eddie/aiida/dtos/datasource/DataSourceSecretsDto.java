package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.UUID;

public record DataSourceSecretsDto(
        @Nullable @JsonProperty UUID dataSourceId,
        @Nullable @JsonProperty String plaintextPassword
) { }
