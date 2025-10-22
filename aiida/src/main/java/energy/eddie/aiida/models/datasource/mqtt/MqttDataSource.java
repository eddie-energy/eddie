package energy.eddie.aiida.models.datasource.mqtt;

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
import jakarta.persistence.PostPersist;
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
    @JsonProperty
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

    public void setPassword(String mqttPassword) {
        this.password = mqttPassword;
    }

    public void generateMqttSettings(
            MqttConfiguration config,
            BCryptPasswordEncoder encoder,
            String plaintextPassword
    ) {
        this.internalHost = config.internalHost();
        this.externalHost = config.externalHost();
        this.topic = TOPIC_PREFIX + SecretGenerator.generate();
        this.username = SecretGenerator.generate();
        this.password = encoder.encode(plaintextPassword);
    }

    @PostPersist
    protected void updateTopic() {
        this.topic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
    }
}