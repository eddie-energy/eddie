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
    protected String mqttInternalHost;
    @JsonProperty
    protected String mqttExternalHost;
    @JsonProperty
    protected String mqttSubscribeTopic;
    @JsonProperty
    protected String mqttUsername;
    @JsonIgnore
    protected String mqttPassword;
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

    public String mqttInternalHost() {
        return mqttInternalHost;
    }

    public String mqttExternalHost() {
        return mqttExternalHost;
    }

    public String mqttSubscribeTopic() {
        return mqttSubscribeTopic;
    }

    public String mqttUsername() {
        return mqttUsername;
    }

    public String mqttPassword() {
        return mqttPassword;
    }

    public void setMqttPassword(String mqttPassword) {
        this.mqttPassword = mqttPassword;
    }

    public void generateMqttSettings(MqttConfiguration config, BCryptPasswordEncoder encoder, String plaintextPassword) {
        this.mqttInternalHost = config.internalHost();
        this.mqttExternalHost = config.externalHost();
        this.mqttSubscribeTopic = TOPIC_PREFIX + SecretGenerator.generate();
        this.mqttUsername = SecretGenerator.generate();
        this.mqttPassword = encoder.encode(plaintextPassword);
    }

    @PostPersist
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
    }
}