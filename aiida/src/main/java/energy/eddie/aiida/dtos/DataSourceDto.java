package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;

import java.util.UUID;

public record DataSourceDto(
        @JsonProperty UUID id,
        @JsonProperty String dataSourceType,
        @JsonProperty String asset,
        @JsonProperty String name,
        @JsonProperty boolean enabled,
        @Nullable @JsonProperty String meteringId,
        @Nullable @JsonProperty Integer simulationPeriod,
        @Nullable @JsonProperty DataSourceMqttDto mqttSettings,
        @Nullable @JsonProperty DataSourceModbusDto modbusSettings
) { }
