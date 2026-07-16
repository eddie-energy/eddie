// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.inbound.ProvisioningConnectionDto;
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

    @Nullable
    @JsonProperty
    public String topic() {
        return accessControlEntry == null ? null : accessControlEntry.topic();
    }

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

    public ProvisioningConnectionDto establishServerModeConnection(
            MqttConfiguration mqttConnection,
            String username,
            String encryptedPassword,
            String topic
    ) {
        connection = new MqttConnection(mqttConnection.internalHost(), mqttConnection.externalHost());
        connection.createMqttUser(username, encryptedPassword);
        accessControlEntry = new MqttAccessControlEntry(username, topic);

        return new ProvisioningConnectionDto(mqttConnection.internalHost(), username, encryptedPassword, topic);
    }

    public void clearMqttProvisioning() {
        connection = null;
        accessControlEntry = null;
    }

    @Nullable
    public MqttConnection connection() {
        return connection;
    }

    @Nullable
    public MqttAccessControlEntry accessControlEntry() {
        return accessControlEntry;
    }
}
