package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MQTT")
@SuppressWarnings("NullAway")
public abstract class MqttDataSource extends DataSource {
    @JsonProperty
    private String mqttServerUri;
    @JsonProperty
    private String mqttSubscribeTopic;
    @JsonProperty
    private String mqttUsername;
    @JsonProperty
    private String mqttPassword;

    protected MqttDataSource() {
    }

    public String getMqttServerUri() {
        return mqttServerUri;
    }

    public void setMqttServerUri(String mqttServerUri) {
        this.mqttServerUri = mqttServerUri;
    }

    public String getMqttSubscribeTopic() {
        return mqttSubscribeTopic;
    }

    public void setMqttSubscribeTopic(String mqttSubscribeTopic) {
        this.mqttSubscribeTopic = mqttSubscribeTopic;
    }

    public String getMqttUsername() {
        return mqttUsername;
    }

    public void setMqttUsername(String mqttUsername) {
        this.mqttUsername = mqttUsername;
    }

    public String getMqttPassword() {
        return mqttPassword;
    }

    public void setMqttPassword(String mqttPassword) {
        this.mqttPassword = mqttPassword;
    }
}
