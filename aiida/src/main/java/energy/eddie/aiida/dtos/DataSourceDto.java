package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.annotation.Nullable;

import java.util.UUID;

public record DataSourceDto(
        @JsonProperty UUID id,
        @JsonProperty DataSourceType dataSourceType,
        @JsonProperty AiidaAsset asset,
        @JsonProperty String name,
        @JsonProperty boolean enabled,
        @Nullable @JsonProperty Integer simulationPeriod,
        @Nullable @JsonProperty DataSourceMqttDto mqttSettings
) { }
