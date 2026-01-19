package energy.eddie.aiida.models.datasource.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.util.UUID;

@Entity
@Table(name = "data_source_mqtt_user")
@SuppressWarnings("NullAway")
public class MqttUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonIgnore
    @SuppressWarnings("unused")
    private Long id;

    @Column(nullable = false)
    @Schema(description = "The username this data source uses to connect to the MQTT broker.")
    @JsonProperty
    private String username;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    @SuppressWarnings("NullAway.Init")
    protected MqttUser() {}

    private MqttUser(MqttUser user, String alias) {
        this.id = user.id;
        this.username = user.username;
        this.password = alias;
    }

    public MqttUser(String username, String alias) {
        this.username = username;
        this.password = alias;
    }

    @SuppressWarnings("NullAway")
    public MqttUser(UUID dataSourceId, String password) {
        this.username = dataSourceId.toString();
        this.password = password;
    }

    public MqttUser copyWithAliasAsPassword(String alias) {
        return new MqttUser(this, alias);
    }

    public Long id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    @Transactional
    public void updatePassword(String password) {
        this.password = password;
    }
}