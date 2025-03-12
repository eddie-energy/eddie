package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
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

    protected MqttDataSource(
            DataSourceDto dto,
            UUID userId,
            String mqttServerUri,
            String mqttUsername,
            String mqttPassword
    ) {
        super(dto, userId);
        this.mqttServerUri = mqttServerUri;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;

        this.mqttSubscribeTopic = "aiida/" + MqttSecretGenerator.generate();
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
}
