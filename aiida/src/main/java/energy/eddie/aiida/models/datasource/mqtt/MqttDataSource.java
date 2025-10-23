package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.config.MqttConfiguration;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAclType;
import energy.eddie.api.agnostic.aiida.mqtt.MqttAction;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

@Entity
@SuppressWarnings("NullAway")
public abstract class MqttDataSource extends DataSource {
    protected static final String TOPIC_PREFIX = "aiida/";
    private static final String TOPIC_SUFFIX = "/+";

    @JsonProperty
    protected String internalHost;

    @JsonProperty
    protected String externalHost;

    @JsonIgnore
    protected String topic;

    @JsonProperty
    protected String username;

    @JsonIgnore
    protected String password;

    @Enumerated(EnumType.STRING)
    protected MqttAction action;

    @Enumerated(EnumType.STRING)
    protected MqttAclType aclType;

    @SuppressWarnings("NullAway")
    protected MqttDataSource() {}

    protected MqttDataSource(DataSourceDto dto, UUID userId) {
        super(dto, userId);
        this.action = MqttAction.ALL;
        this.aclType = MqttAclType.ALLOW;
    }

    public String internalHost() {
        return internalHost;
    }

    public String externalHost() {
        return externalHost;
    }

    public String topic() {
        return topic;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public void setPassword(String hashedPassword) {
        this.password = hashedPassword;
    }

    public void generateMqttSettings(
            MqttConfiguration config,
            BCryptPasswordEncoder encoder,
            String plaintextPassword
    ) {
        this.internalHost = config.internalHost();
        this.externalHost = config.externalHost();
        this.password = encoder.encode(plaintextPassword);
    }

    /**
     * This method formats the topic to correctly display it to the user in the UI.
     * This topic can then be copied by the user without making any changes to it.
     *
     * @return The topic formatted for the UI
     */
    @JsonGetter("topic")
    protected String topicFormattedForUi() {
        return this.topic.replace(TOPIC_SUFFIX, "");
    }

    protected void generateTopic() {
        this.topic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
    }

    protected void generateUsername() {
        this.username = id.toString();
    }

    /**
     * This method generates the Topic and Username for a MQTT data source using the ID of the data source.
     * This is a lifecycle event and is executed before persisting it to the database.
     *
     * This is the default behaviour of a MQTT data source.
     */
    @PrePersist
    protected void generateTopicAndUsername() {
        generateTopic();
        generateUsername();
    }
}