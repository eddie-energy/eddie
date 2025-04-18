package energy.eddie.aiida.models.permission;

import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "mqtt_streaming_config")
public class MqttStreamingConfig {
    @Id
    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
    @Column(name = "username", nullable = false)
    private String username;
    @Column(name = "password", nullable = false)
    private String password;
    @Column(name = "server_uri", nullable = false)
    private String serverUri;
    @Column(name = "data_topic", nullable = false)
    private String dataTopic;
    @Column(name = "status_topic", nullable = false)
    private String statusTopic;
    @Column(name = "termination_topic", nullable = false)
    private String terminationTopic;

    @SuppressWarnings("NullAway.Init") // required by JPA
    protected MqttStreamingConfig() {
    }

    public MqttStreamingConfig(UUID permissionId, MqttDto mqttDto) {
        this.permissionId = permissionId;
        this.username = mqttDto.username();
        this.password = mqttDto.password();
        this.serverUri = mqttDto.serverUri();
        this.dataTopic = mqttDto.dataTopic();
        this.statusTopic = mqttDto.statusTopic();
        this.terminationTopic = mqttDto.terminationTopic();
    }

    public UUID permissionId() {
        return permissionId;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public String serverUri() {
        return serverUri;
    }

    public String dataTopic() {
        return dataTopic;
    }

    public String statusTopic() {
        return statusTopic;
    }

    public String terminationTopic() {
        return terminationTopic;
    }
}
