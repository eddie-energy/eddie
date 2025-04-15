package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSource;
import jakarta.persistence.Entity;
import jakarta.persistence.PostPersist;

import java.util.UUID;

@Entity
@SuppressWarnings("NullAway")
public abstract class MqttDataSource extends DataSource {
    protected static final String TOPIC_PREFIX = "aiida/";
    private static final String TOPIC_POSTFIX = "/+";

    @JsonProperty
    protected String mqttInternalHost;
    @JsonProperty
    protected String mqttExternalHost;
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
        this.mqttInternalHost = dataSourceMqttDto.internalHost();
        this.mqttExternalHost = dataSourceMqttDto.externalHost();
        this.mqttSubscribeTopic = dataSourceMqttDto.subscribeTopic();
        this.mqttUsername = dataSourceMqttDto.username();
        this.mqttPassword = dataSourceMqttDto.password();
    }

    @Override
    public DataSource mergeWithDto(DataSourceDto dto, UUID userId) {
        var mqttSettingsDto = new DataSourceMqttDto(mqttInternalHost(),
                                                    mqttExternalHost(),
                                                    mqttSubscribeTopic(),
                                                    mqttUsername(),
                                                    mqttPassword());
        return createFromDto(dto, userId, mqttSettingsDto);
    }

    @Override
    public DataSourceDto toDto() {
        return new DataSourceDto(
                id,
                dataSourceType,
                asset,
                name,
                enabled,
                null,
                toMqttDto()
        );
    }

    public String mqttInternalHost() {
        return mqttInternalHost;
    }

    public String mqttExternalHost() {
        return mqttExternalHost;
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

    /**
     * Returns the length of the MQTT subscribe topic without the wildcard.
     *
     * @return the length of the MQTT subscribe topic without the wildcard
     */
    public int mqttSubscribeTopicLengthWithoutWildcard() {
        return mqttSubscribeTopic.length() - 1;
    }

    public DataSourceMqttDto toMqttDto() {
        return new DataSourceMqttDto(mqttInternalHost,
                                     mqttExternalHost,
                                     mqttSubscribeTopic,
                                     mqttUsername,
                                     mqttPassword);
    }

    @PostPersist
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = TOPIC_PREFIX + id + TOPIC_POSTFIX;
    }
}