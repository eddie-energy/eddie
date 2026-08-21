// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt.inbound;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.mqtt.MqttConnection;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;

@Entity
@Table(name = "data_source_mqtt_inbound_provisioning")
public class InboundProvisioningMqttConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonIgnore
    @SuppressWarnings("unused")
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "mqtt_acl_id", nullable = false, referencedColumnName = "id")
    @JsonIgnore
    private MqttAccessControlEntry accessControlEntry;

    @Schema(description = "The MQTT connection used for inbound provisioning.")
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "mqtt_connection_id", nullable = false, referencedColumnName = "id")
    @JsonProperty
    private MqttConnection connection;

    @SuppressWarnings("NullAway")
    protected InboundProvisioningMqttConfig() {}

    @SuppressWarnings("NullAway")
    private InboundProvisioningMqttConfig(
            MqttConnection connection,
            MqttAccessControlEntry accessControlEntry
    ) {
        this.connection = connection;
        this.accessControlEntry = accessControlEntry;
    }

    public static InboundProvisioningMqttConfig create(
            String internalHost,
            String externalHost,
            String username,
            String password,
            String topic
    ) {
        var connection = new MqttConnection(internalHost, externalHost);
        connection.createMqttUser(username, password);

        return new InboundProvisioningMqttConfig(
                connection,
                new MqttAccessControlEntry(username, topic)
        );
    }

    @JsonProperty
    public String topic() {
        return accessControlEntry.topic();
    }

    public MqttConnection connection() {
        return connection;
    }

    public MqttAccessControlEntry accessControlEntry() {
        return accessControlEntry;
    }
}
