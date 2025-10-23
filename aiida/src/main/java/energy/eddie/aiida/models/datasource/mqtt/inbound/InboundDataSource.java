package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.Objects;
import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.INBOUND)
public class InboundDataSource extends MqttDataSource {
    @JsonProperty
    protected String accessCode;

    @SuppressWarnings("NullAway")
    protected InboundDataSource() {}

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, MqttStreamingConfig mqttStreamingConfig) {
        this(dto, userId, mqttStreamingConfig, SecretGenerator.generate());
    }

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, MqttStreamingConfig mqttStreamingConfig, String accessCode) {
        super(dto, userId);
        this.internalHost = mqttStreamingConfig.serverUri();
        this.externalHost = mqttStreamingConfig.serverUri();
        this.topic = mqttStreamingConfig.dataTopic();
        this.username = mqttStreamingConfig.username();
        this.password = mqttStreamingConfig.password();
        this.accessCode = accessCode;
    }

    public static class Builder  {
        private final InboundDataSourceDto dataSourceDto;
        private final UUID userId;
        private final MqttStreamingConfig mqttStreamingConfig;

        @SuppressWarnings("NullAway")
        public Builder(Permission permission) {
            this.userId = Objects.requireNonNull(permission.userId());
            var dataNeed = Objects.requireNonNull(permission.dataNeed());
            this.mqttStreamingConfig = Objects.requireNonNull(permission.mqttStreamingConfig());

            this.dataSourceDto = new InboundDataSourceDto(dataNeed.asset(), permission.id());
        }

        public InboundDataSource build() {
            return new InboundDataSource(dataSourceDto, userId, mqttStreamingConfig);
        }
    }

    @Override
    protected void generateTopicAndUsername() {
        // Ignore, as the username and the topic are set in the constructor
    }

    public String accessCode() {
        return accessCode;
    }
}
