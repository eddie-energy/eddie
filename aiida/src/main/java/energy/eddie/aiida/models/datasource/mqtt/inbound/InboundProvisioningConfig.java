// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.provisioning.ProvisioningConnectionDto;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.mqtt.MqttConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import javax.annotation.Nullable;

@Embeddable
public class InboundProvisioningConfig {

    private static final String TABLE_NAME = "data_source_mqtt_inbound";

    @Nullable
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_acl_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonIgnore
    protected MqttAccessControlEntry accessControlEntry;

    @Nullable
    @Schema(description = "The mqtt connection when provisioning type is client mode.")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_connection_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonProperty
    private MqttConnection connection;

    /**
     * Returns the topic from the current provisioning access-control entry.
     *
     * @return The configured provisioning topic, or {@code null} when MQTT provisioning is not configured.
     */
    @Nullable
    @JsonProperty
    public String topic() {
        return accessControlEntry == null ? null : accessControlEntry.topic();
    }

    /**
     * Stores an MQTT client-mode connection and matching publish access-control entry.
     *
     * @param internalHost Broker host used internally by the publisher.
     * @param externalHost Broker host exposed to the provisioning client.
     * @param username     MQTT username used for publishing.
     * @param password     MQTT password used for publishing.
     * @param topic        Topic to which inbound records are published.
     * @return Connection details for the configured MQTT client mode.
     */
    public ProvisioningConnectionDto establishClientModeConnection(
            String internalHost,
            String externalHost,
            String username,
            String password,
            String topic
    ) {
        connection = new MqttConnection(internalHost, externalHost);
        connection.createMqttUser(username, password);
        accessControlEntry = new MqttAccessControlEntry(username, topic);

        return new ProvisioningConnectionDto(internalHost, username, password, topic);
    }

    /**
     * Stores an MQTT server-mode connection and matching publish access-control entry.
     *
     * @param mqttConnection    MQTT broker configuration supplying the internal and external hosts.
     * @param username          Generated MQTT username used for publishing.
     * @param encryptedPassword Encoded MQTT password stored for the generated user.
     * @param plaintextPassword Plaintext MQTT password returned once to the provisioning client.
     * @param topic             Topic to which inbound records are published.
     * @return Connection details for the configured MQTT server mode.
     */
    public ProvisioningConnectionDto establishServerModeConnection(
            MqttConfiguration mqttConnection,
            String username,
            String encryptedPassword,
            String plaintextPassword,
            String topic
    ) {
        connection = new MqttConnection(mqttConnection.internalHost(), mqttConnection.externalHost());
        connection.createMqttUser(username, encryptedPassword);
        accessControlEntry = new MqttAccessControlEntry(username, topic);

        return new ProvisioningConnectionDto(mqttConnection.externalHost(), username, plaintextPassword, topic);
    }

    /**
     * Removes the MQTT connection and access-control entry from this provisioning configuration.
     */
    public void clearMqttProvisioning() {
        connection = null;
        accessControlEntry = null;
    }

    /**
     * Returns the current MQTT provisioning connection.
     *
     * @return The provisioning connection, or {@code null} when MQTT provisioning is not configured.
     */
    @Nullable
    public MqttConnection connection() {
        return connection;
    }

    /**
     * Returns the current MQTT provisioning access-control entry.
     *
     * @return The access-control entry, or {@code null} when MQTT provisioning is not configured.
     */
    @Nullable
    public MqttAccessControlEntry accessControlEntry() {
        return accessControlEntry;
    }
}
