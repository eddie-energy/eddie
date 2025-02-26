package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record DataSourceDto (
        @JsonProperty UUID id,
        @JsonProperty String dataSourceType,
        @JsonProperty String asset,
        @JsonProperty String name,
        @JsonProperty boolean enabled,
        @JsonProperty String mqttServerUri,
        @JsonProperty String mqttSubscribeTopic,
        @JsonProperty String mqttUsername,
        @JsonProperty String mqttPassword,
        @JsonProperty String meteringId,
        @JsonProperty Integer simulationPeriod) { }
