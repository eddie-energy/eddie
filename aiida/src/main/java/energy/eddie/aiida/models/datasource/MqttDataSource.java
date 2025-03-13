package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@SuppressWarnings("NullAway")
public abstract class MqttDataSource extends DataSource {
    @JsonProperty
    protected String mqttServerUri;
    @JsonProperty
    protected String mqttSubscribeTopic;
    @JsonProperty
    protected String mqttUsername;
    @JsonProperty
    protected String mqttPassword;

    @SuppressWarnings("NullAway")
    protected MqttDataSource() {}

    protected MqttDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId);
        this.mqttServerUri = dataSourceMqttDto.serverUri();
        this.mqttSubscribeTopic = dataSourceMqttDto.subscribeTopic();
        this.mqttUsername = dataSourceMqttDto.username();
        this.mqttPassword = dataSourceMqttDto.password();
    }

    public String mqttServerUri() {
        return mqttServerUri;
    }

    public String mqttSubscribeTopic() {
        return mqttSubscribeTopic;
    }

    public String mqttUsername() {
        return mqttUsername;
    }

    public String mqttPassword() {
        return mqttPassword;
    }

    @Override
    public DataSourceDto toDto() {
        return new DataSourceDto(
                id,
                dataSourceType.identifier(),
                asset.asset(),
                name,
                enabled,
                null,
                null,
                toMqttDto()
        );
    }

    public DataSourceMqttDto toMqttDto() {
        return new DataSourceMqttDto(mqttServerUri, mqttSubscribeTopic, mqttUsername, mqttPassword);
    }
}
