package energy.eddie.aiida.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataSourceMqttDto(
        @JsonProperty String serverUri,
        @JsonProperty String subscribeTopic,
        @JsonProperty String username,
        @JsonProperty String password
) {
    public DataSourceMqttDto() {
        this("", "", "", "");
    }
}
