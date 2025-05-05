package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataSourceMqttDto(
        @JsonProperty String internalHost,
        @JsonProperty String externalHost,
        @JsonProperty String subscribeTopic,
        @JsonProperty String username,
        @JsonProperty String password
) implements DataSourceProtocolSettings {
    public DataSourceMqttDto() {
        this("", "", "", "", "");
    }
}
