// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.dtos.inbound.ProvisioningConnectionDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import energy.eddie.aiida.models.mqtt.MqttConnection;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.aiida.services.secrets.KeyStoreSecretsService.alias;

@Entity
@SecondaryTable(name = InboundDataSource.TABLE_NAME)
@DiscriminatorValue(DataSourceType.Identifiers.INBOUND)
public class InboundDataSource extends MqttDataSource {
    protected static final String TABLE_NAME = "data_source_mqtt_inbound";

    @JsonIgnore
    protected String serverModeTopic;

    @Column(name = "access_code", table = TABLE_NAME)
    @Schema(description = "The access code to retrieve the inbound data.")
    @JsonProperty
    protected String accessCode;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", table = TABLE_NAME)
    @JsonIgnore
    protected Permission permission;

    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_type", table = TABLE_NAME, nullable = false)
    @Schema(description = "The type defining how inbound data will be provided.")
    private InboundProvisioningType provisioningType;

    @Transient
    @JsonIgnore
    private MqttStreamingConfig config;

    @Embedded
    @JsonProperty("provisioningConfig")
    private InboundProvisioningConfig inboundProvisioningConfig;

    @SuppressWarnings("NullAway")
    protected InboundDataSource() {}

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission) {
        this(dto, userId, permission, SecretGenerator.generate());
    }

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission, String accessCode) {
        super(dto, userId);
        this.permission = permission;
        this.config = Objects.requireNonNull(permission.mqttStreamingConfig());

        this.mqttConnection = new MqttConnection(config.serverUri(), config.serverUri());

        this.accessCode = accessCode;
        this.provisioningType = InboundProvisioningType.REST_BEARER;
        this.inboundProvisioningConfig = new InboundProvisioningConfig();
        setServerModeTopic();
    }

    public String accessCode() {
        return accessCode;
    }

    public void updateAccessCode(String accessCode) {
        this.accessCode = accessCode;
    }

    public Permission permission() {
        return permission;
    }

    @JsonProperty
    public Set<AiidaSchema> schemas() {
        var dataNeed = permission == null ? null : permission.dataNeed();
        return dataNeed == null ? Set.of() : dataNeed.schemas();
    }

    @Nullable
    public String acknowledgementTopic() {
        return config != null ? config.acknowledgementTopic() : null;
    }

    public InboundProvisioningType inboundProvisioningType() {
        return provisioningType;
    }

    public MqttConnection provisioningConnection() {
        return Objects.requireNonNull(
                inboundProvisioningConfig.connection(),
                "Provisioning MQTT connection is not configured"
        );
    }

    public String provisioningTopicOrThrow() {
        var accessControlEntry = Objects.requireNonNull(
                inboundProvisioningConfig.accessControlEntry(),
                "Provisioning MQTT ACL is not configured"
        );
        return accessControlEntry.topic();
    }

    @Transactional
    public void changeInboundProvisioningType(InboundProvisioningType inboundProvisioningType) {
        this.provisioningType = inboundProvisioningType;

        if (inboundProvisioningType == InboundProvisioningType.REST_API_TOKEN ||
            inboundProvisioningType == InboundProvisioningType.REST_BEARER) {
            inboundProvisioningConfig.clearMqttProvisioning();
        }
    }

    @Transactional
    public ProvisioningConnectionDto establishClientModeConnection(
            String host,
            String username,
            String password,
            String topic
    ) {
        changeInboundProvisioningType(InboundProvisioningType.MQTT_CLIENT);
        return inboundProvisioningConfig.establishClientModeConnection(host, host, username, password, topic);
    }

    @Transactional
    public ProvisioningConnectionDto establishServerModeConnection(
            MqttConfiguration mqttConfig,
            BCryptPasswordEncoder encoder,
            String plaintextPassword
    ) {
        changeInboundProvisioningType(InboundProvisioningType.MQTT_SERVER);
        var username = UUID.randomUUID().toString();
        var password = Objects.requireNonNull(encoder.encode(plaintextPassword));

        return inboundProvisioningConfig.establishServerModeConnection(mqttConfig, username, password, serverModeTopic);
    }

    @Override
    protected void createMqttUser() {
        this.mqttConnection.createMqttUser(config.username().toString(), alias(id, SecretType.PASSWORD));
    }

    @Override
    protected void postPersist() {
        super.postPersist();
        this.accessCode = alias(id, SecretType.API_KEY);
    }

    @Override
    protected void createAccessControlEntry() {
        this.accessControlEntry = new MqttAccessControlEntry(config.username().toString(), config.dataTopic());
    }

    private void setServerModeTopic() {
        serverModeTopic = TOPIC_PREFIX + id + "/inboundData";
    }

    public static class Builder {
        private final InboundDataSourceDto dataSourceDto;
        private final UUID userId;
        private final Permission permission;

        @SuppressWarnings("NullAway")
        public Builder(Permission permission) {
            this.userId = Objects.requireNonNull(permission.userId());

            this.permission = Objects.requireNonNull(permission);

            var dataNeed = Objects.requireNonNull(permission.dataNeed());
            this.dataSourceDto = new InboundDataSourceDto(dataNeed.asset(), permission.id());
        }

        public InboundDataSource build() {
            return new InboundDataSource(dataSourceDto, userId, permission);
        }
    }
}
