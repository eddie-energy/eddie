package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DataSourceSecretsDto(
        @JsonProperty UUID dataSourceId,
        @JsonProperty String plaintextPassword
) {}
