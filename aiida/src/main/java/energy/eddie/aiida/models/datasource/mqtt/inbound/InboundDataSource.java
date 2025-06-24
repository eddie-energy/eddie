package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
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

    public InboundDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId, dataSourceMqttDto);
        this.accessCode = SecretGenerator.generate();
    }

    public static class Builder  {
        private final DataSourceDto dataSourceDto;
        private final UUID userId;
        private final DataSourceMqttDto dataSourceMqttDto;

        @SuppressWarnings("NullAway")
        public Builder(Permission permission) {
            this.userId = Objects.requireNonNull(permission.userId());
            var dataNeed = Objects.requireNonNull(permission.dataNeed());
            var mqttStreamingConfig = Objects.requireNonNull(permission.mqttStreamingConfig());

            this.dataSourceDto = new DataSourceDto(
                    null,
                    DataSourceType.INBOUND,
                    dataNeed.asset(),
                    permission.permissionId().toString(),
                    true,
                    null, null, null
            );

            this.dataSourceMqttDto = new DataSourceMqttDto(
                    mqttStreamingConfig.serverUri(),
                    mqttStreamingConfig.serverUri(),
                    mqttStreamingConfig.dataTopic(),
                    mqttStreamingConfig.username(),
                    mqttStreamingConfig.password()
            );
        }

        public InboundDataSource build() {
            return new InboundDataSource(dataSourceDto, userId, dataSourceMqttDto);
        }
    }

    @Override
    protected void updateMqttSubscribeTopic() {
        // Keep the subscribe topic as is, since it is set during the creation of the data source
    }

    public String accessCode() {
        return accessCode;
    }
}
