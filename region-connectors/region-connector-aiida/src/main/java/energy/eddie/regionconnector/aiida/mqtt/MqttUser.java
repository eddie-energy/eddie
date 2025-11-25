package energy.eddie.regionconnector.aiida.mqtt;

import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "aiida_mqtt_user", schema = AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID)
public class MqttUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private String id;
    @Column(name = "username", nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "is_superuser", nullable = false)
    private boolean isSuperuser;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @SuppressWarnings("NullAway")
    protected MqttUser() {
    }

    @SuppressWarnings("NullAway")
    public MqttUser(
            String permissionId,
            String passwordHash,
            boolean isSuperuser
    ) {
        this.username = permissionId;
        this.passwordHash = passwordHash;
        this.isSuperuser = isSuperuser;
    }

    public String id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public boolean isSuperuser() {
        return isSuperuser;
    }

    public String permissionId() {
        return username;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
