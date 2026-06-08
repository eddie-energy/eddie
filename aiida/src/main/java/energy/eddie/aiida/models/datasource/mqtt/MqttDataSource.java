// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.mqtt.MqttConnectionEntity;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@Entity
@SecondaryTable(name = MqttDataSource.TABLE_NAME)
public abstract class MqttDataSource extends DataSource {
    public static final String TABLE_NAME = "data_source_mqtt";
    protected static final String TOPIC_PREFIX = "aiida/";
    private static final String TOPIC_SUFFIX = "/+";

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_connection_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonUnwrapped
    protected MqttConnectionEntity mqttConnection;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_acl_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonIgnore
    protected MqttAccessControlEntry accessControlEntry;

    @Transient
    @JsonIgnore
    private String passwordHash;

    @SuppressWarnings("NullAway")
    protected MqttDataSource() {}

    @SuppressWarnings("NullAway")
    protected MqttDataSource(DataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    public String internalHost() {
        return mqttConnection.internalHost();
    }

    public String topic() {
        return accessControlEntry.topic();
    }

    public String username() {
        return mqttConnection.username();
    }

    public String password() {
        return mqttConnection.password();
    }

    @SuppressWarnings("NullAway")
    // False positive for encode call, since plaintextPassword is NonNull encode should always return non-null password
    public void configure(MqttConfiguration config, BCryptPasswordEncoder encoder, String plaintextPassword) {
        this.mqttConnection = new MqttConnectionEntity(config.internalHost(), config.externalHost());
        this.passwordHash = encoder.encode(plaintextPassword);
    }

    @SuppressWarnings("NullAway")
    // False positive for encode call, since plaintextPassword is NonNull encode should always return non-null password
    public void updatePassword(BCryptPasswordEncoder encoder, String plaintextPassword) {
        var password = encoder.encode(plaintextPassword);
        this.mqttConnection.updatePassword(password);
    }

    @PrePersist
    @Transactional
    protected void postPersist() {
        createAccessControlEntry();
        createMqttUser();
    }

    @Transactional
    protected void createMqttUser() {
        mqttConnection.createMqttUser(id.toString(), passwordHash);
    }

    /**
     * This method creates the Access Control Entry for a MQTT data source using the ID of the data source.
     * This is the default behaviour of a MQTT data source.
     */
    @Transactional
    protected void createAccessControlEntry() {
        var topic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
        accessControlEntry = new MqttAccessControlEntry(id, topic);
    }

    /**
     * This method formats the topic to correctly display it to the user in the UI.
     * This topic can then be copied by the user without making any changes to it.
     *
     * @return The topic formatted for the UI
     */
    @JsonGetter(value = "topic")
    protected String topicFormattedForUi() {
        return accessControlEntry.topic().replace(TOPIC_SUFFIX, "");
    }
}