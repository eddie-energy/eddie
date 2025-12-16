package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @Column(name = "internal_host", table = TABLE_NAME, nullable = false)
    @Schema(description = "The internal host of the MQTT broker the data source connects to.")
    @JsonProperty
    protected String internalHost;

    @Column(name = "external_host", table = TABLE_NAME, nullable = false)
    @Schema(description = "The external host of the MQTT broker the data source connects to.")
    @JsonProperty
    protected String externalHost;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "mqtt_user_id", table = TABLE_NAME, referencedColumnName = "id")
    @JsonUnwrapped
    protected MqttUser user;

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

    protected MqttDataSource(MqttDataSource mqttDataSource) {
        super(mqttDataSource);
        internalHost = mqttDataSource.internalHost;
        externalHost = mqttDataSource.externalHost;
        user = mqttDataSource.user;
        accessControlEntry = mqttDataSource.accessControlEntry;
        passwordHash = mqttDataSource.passwordHash;
    }

    public String internalHost() {
        return internalHost;
    }

    public String topic() {
        return accessControlEntry.topic();
    }

    public String username() {
        return user.username();
    }

    public String password() {
        return user.password();
    }

    public void configure(MqttConfiguration config, BCryptPasswordEncoder encoder, String plaintextPassword) {
        this.internalHost = config.internalHost();
        this.externalHost = config.externalHost();
        this.passwordHash = encoder.encode(plaintextPassword);
    }

    public void updatePassword(BCryptPasswordEncoder encoder, String plaintextPassword) {
        var password = encoder.encode(plaintextPassword);
        this.user.updatePassword(password);
    }

    @PrePersist
    @Transactional
    protected void prePersist() {
        createAccessControlEntry();
        createMqttUser();
    }

    @Transactional
    protected void createMqttUser() {
        this.user = new MqttUser(id, passwordHash);
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