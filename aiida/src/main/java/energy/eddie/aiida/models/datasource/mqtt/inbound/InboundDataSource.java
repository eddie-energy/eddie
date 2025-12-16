package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.MqttUser;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.secrets.SecretType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

import java.util.Objects;
import java.util.UUID;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;

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

    @SuppressWarnings("NullAway")
    public InboundDataSource(
            InboundDataSourceDto dto,
            UUID userId,
            MqttStreamingConfig mqttStreamingConfig
    ) {
        super(dto, userId);
        this.config = mqttStreamingConfig;
        this.internalHost = config.serverUri();
        this.externalHost = config.serverUri();
    }

    private InboundDataSource(InboundDataSource inboundDataSource, String passwordAlias, String accessCodeAlias) {
        super(inboundDataSource);
        accessCode = accessCodeAlias;
        config = inboundDataSource.config;
        user = inboundDataSource.user.copyWithAliasAsPassword(passwordAlias);
    }

    public String accessCode() {
        return accessCode;
    }

    public InboundDataSource copyWithAliasAsPassword() {
        return new InboundDataSource(this, alias(id, SecretType.PASSWORD), alias(id, SecretType.API_KEY));
    }

    @Override
    protected void prePersist() {
        super.prePersist();
        createAccessCode();
    }

    @Override
    protected void createMqttUser() {
        this.user = new MqttUser(config.username().toString(), alias(id, SecretType.PASSWORD));
    }

    @Override
    protected void createAccessControlEntry() {
        this.accessControlEntry = new MqttAccessControlEntry(config.username().toString(), config.dataTopic());
    }

    private void createAccessCode() {
        this.accessCode = alias(id, SecretType.API_KEY);
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
