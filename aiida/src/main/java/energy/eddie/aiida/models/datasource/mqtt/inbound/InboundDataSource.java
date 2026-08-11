// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.mqtt.inbound.InboundDataSourceDto;
import energy.eddie.aiida.dtos.provisioning.MqttProvisioningConnectionDto;
import energy.eddie.aiida.errors.inbound.ProvisioningConfigurationException;
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
    @JsonProperty
    private InboundProvisioningType provisioningType;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(
            name = "permission_id",
            table = TABLE_NAME,
            referencedColumnName = "permission_id",
            insertable = false,
            updatable = false
    )
    @JsonIgnore
    private MqttStreamingConfig config;

    @Nullable
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_provisioning_config_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonProperty("mqttProvisioningConfig")
    private InboundProvisioningMqttConfig inboundProvisioningMqttConfig;

    @SuppressWarnings("NullAway")
    protected InboundDataSource() {}

    /**
     * Configures MQTT server-mode provisioning using the local broker configuration and activates that mode. A unique
     * username is created and the supplied plaintext password is encoded before it is stored.
     *
     * @param mqttConfig        Local MQTT broker configuration.
     * @param encoder           Encoder used to protect the generated credential before storage.
     * @param plaintextPassword Plaintext password generated for the provisioning connection.
     * @return The connection details stored for server-mode provisioning.
     */
    @Transactional
    public MqttProvisioningConnectionDto establishServerModeConnection(
            MqttConfiguration mqttConfig,
            BCryptPasswordEncoder encoder,
            String plaintextPassword
    ) {
        var username = UUID.randomUUID().toString();
        var password = Objects.requireNonNull(encoder.encode(plaintextPassword));
        var serverModeTopic = TOPIC_PREFIX + username + "/inboundData";
        this.provisioningType = InboundProvisioningType.MQTT_SERVER;
        this.inboundProvisioningMqttConfig = InboundProvisioningMqttConfig.create(
                mqttConfig.internalHost(),
                mqttConfig.externalHost(),
                username,
                password,
                serverModeTopic
        );

        return new MqttProvisioningConnectionDto(
                mqttConfig.externalHost(),
                username,
                plaintextPassword,
                serverModeTopic
        );
    }

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission) {
        this(dto, userId, permission, SecretGenerator.generate());
    }

    public InboundDataSource(InboundDataSourceDto dto, UUID userId, Permission permission, String accessCode) {
        super(dto, userId);
        this.permission = permission;
        this.config = Objects.requireNonNull(permission.mqttStreamingConfig());

        this.mqttConnection = new MqttConnection(config.serverUri(), config.serverUri());

        this.accessCode = accessCode;
        this.provisioningType = InboundProvisioningType.NONE;
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
        return config.acknowledgementTopic();
    }

    public InboundProvisioningType inboundProvisioningType() {
        return provisioningType;
    }

    public MqttConnection provisioningConnection() throws ProvisioningConfigurationException {
        return mqttProvisioningConfig().connection();
    }

    public String provisioningTopic() throws ProvisioningConfigurationException {
        return mqttProvisioningConfig().accessControlEntry().topic();
    }

    /**
     * Changes the inbound provisioning type. Switching to a REST type also removes any stored MQTT provisioning
     * connection and access-control entry.
     */
    @Transactional
    public void changeInboundProvisioningType(InboundProvisioningType inboundProvisioningType) {
        this.provisioningType = inboundProvisioningType;

        if (inboundProvisioningType == InboundProvisioningType.REST_API_TOKEN ||
            inboundProvisioningType == InboundProvisioningType.REST_BEARER) {
            inboundProvisioningMqttConfig = null;
        }
    }

    /**
     * Configures MQTT client-mode provisioning with externally supplied broker credentials and activates that mode.
     *
     * @return The connection details stored for client-mode provisioning.
     */
    @Transactional
    public MqttProvisioningConnectionDto establishClientModeConnection(
            String host,
            String username,
            String password,
            String topic
    ) {
        this.provisioningType = InboundProvisioningType.MQTT_CLIENT;
        this.inboundProvisioningMqttConfig = InboundProvisioningMqttConfig.create(
                host,
                host,
                username,
                password,
                topic
        );
        return new MqttProvisioningConnectionDto(host, username, password, topic);
    }

    @Transactional
    public MqttProvisioningConnectionDto resetServerModePassword(
            BCryptPasswordEncoder encoder,
            String plaintextPassword
    ) throws ProvisioningConfigurationException {
        var provisioningConfig = mqttProvisioningConfig();
        var connection = provisioningConfig.connection();
        var encodedPassword = Objects.requireNonNull(encoder.encode(plaintextPassword));
        connection.updatePassword(encodedPassword);

        return new MqttProvisioningConnectionDto(
                connection.externalHost(),
                connection.username(),
                plaintextPassword,
                provisioningConfig.topic()
        );
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
        this.accessControlEntry = new MqttAccessControlEntry(
                config.username().toString(),
                config.dataTopic()
        );
    }

    private InboundProvisioningMqttConfig mqttProvisioningConfig()
            throws ProvisioningConfigurationException {
        if (inboundProvisioningMqttConfig != null) {
            return inboundProvisioningMqttConfig;
        }

        throw new ProvisioningConfigurationException(id, provisioningType);
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
