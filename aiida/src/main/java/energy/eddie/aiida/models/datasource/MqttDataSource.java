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
                toMqttDto(),
                null
        );
    }

    public DataSourceMqttDto toMqttDto() {
        return new DataSourceMqttDto(mqttInternalHost, mqttExternalHost, mqttSubscribeTopic, mqttUsername, mqttPassword);
    }
}
