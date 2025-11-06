package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.MqttUser;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@SecondaryTable(name = InboundDataSource.TABLE_NAME)
@DiscriminatorValue(DataSourceType.Identifiers.INBOUND)
public class InboundDataSource extends MqttDataSource {
    protected static final String TABLE_NAME = "data_source_mqtt_inbound";

    @Column(name = "access_code", table = TABLE_NAME)
    @Schema(description = "The access code to retrieve the inbound data.")
    @JsonProperty
    protected String accessCode;

    @Transient
    @JsonIgnore
    private MqttStreamingConfig config;

    @SuppressWarnings("NullAway")
    protected InboundDataSource() {}

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, MqttStreamingConfig mqttStreamingConfig) {
        this(dto, userId, mqttStreamingConfig, SecretGenerator.generate());
    }

    public InboundDataSource(
            InboundDataSourceDto dto,
            UUID userId,
            MqttStreamingConfig mqttStreamingConfig,
            String accessCode
    ) {
        super(dto, userId);
        this.config = mqttStreamingConfig;
        this.internalHost = config.serverUri();
        this.externalHost = config.serverUri();
        this.accessCode = accessCode;
    }

    public String accessCode() {
        return accessCode;
    }

    @Override
    protected void createMqttUser() {
        this.user = new MqttUser(config.username().toString(), config.password());
    }

    @Override
    protected void createAccessControlEntry() {
        this.accessControlEntry = new MqttAccessControlEntry(config.username().toString(), config.dataTopic());
    }

    public static class Builder {
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
}
