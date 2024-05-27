package energy.eddie.aiida.models.permission;

import energy.eddie.api.agnostic.aiida.MqttDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "mqtt_streaming_config")
public class MqttStreamingConfig {
    @Id
    @Column(name = "permission_id", nullable = false)
    private String permissionId;
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

    public MqttStreamingConfig(
            String permissionId,
            String username,
            String password,
            String serverUri,
            String dataTopic,
            String statusTopic,
            String terminationTopic
    ) {
        this.permissionId = permissionId;
        this.username = username;
        this.password = password;
        this.serverUri = serverUri;
        this.dataTopic = dataTopic;
        this.statusTopic = statusTopic;
        this.terminationTopic = terminationTopic;
    }

    @SuppressWarnings("NullAway.Init") // required by JPA
    protected MqttStreamingConfig() {
    }

    public MqttStreamingConfig(String permissionId, MqttDto mqttDto) {
        this.permissionId = permissionId;
        this.username = mqttDto.username();
        this.password = mqttDto.password();
        this.serverUri = mqttDto.serverUri();
        this.dataTopic = mqttDto.dataTopic();
        this.statusTopic = mqttDto.statusTopic();
        this.terminationTopic = mqttDto.terminationTopic();
    }

    public String permissionId() {
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
