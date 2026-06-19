// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import energy.eddie.aiida.models.datasource.mqtt.MqttUser;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

@Entity
@Table(name = "mqtt_connection")
public class MqttConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonIgnore
    @SuppressWarnings("unused")
    private Long id;

    @Column(name = "internal_host", nullable = false)
    @Schema(description = "The internal host of the MQTT broker the data source connects to.")
    @JsonProperty
    private String internalHost;

    @Column(name = "external_host", nullable = false)
    @Schema(description = "The external host of the MQTT broker the data source connects to.")
    @JsonProperty
    private String externalHost;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_user_id", referencedColumnName = "id")
    @JsonUnwrapped
    private MqttUser user;

    @SuppressWarnings("NullAway.Init")
    public MqttConnection() {}

    @SuppressWarnings("NullAway")
    public MqttConnection(String internalHost, String externalHost) {
        this.internalHost = internalHost;
        this.externalHost = externalHost;
    }

    public String internalHost() {
        return internalHost;
    }

    public String username() {
        return user.username();
    }

    public String password() {
        return user.password();
    }

    @Transactional
    public void createMqttUser(String username, String password) {
        user = new MqttUser(username, password);
    }

    @Transactional
    public void updatePassword(String password) {
        user.updatePassword(password);
    }
}
