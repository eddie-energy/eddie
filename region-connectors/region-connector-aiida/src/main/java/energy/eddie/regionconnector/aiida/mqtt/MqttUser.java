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
    private final String id;
    @Column(name = "username", nullable = false, unique = true)
    private final String username;
    @Column(name = "password_hash", nullable = false)
    private final String passwordHash;
    @Column(name = "is_superuser", nullable = false)
    private final boolean isSuperuser;
    @Column(name = "permission_id", nullable = false, unique = true)
    private final String permissionId;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private final Instant createdAt;

    @SuppressWarnings("NullAway")
    protected MqttUser() {
        this.id = null;
        this.username = null;
        this.passwordHash = null;
        this.isSuperuser = false;
        this.permissionId = null;
        this.createdAt = null;
    }

    @SuppressWarnings("NullAway")
    public MqttUser(
            String username,
            String passwordHash,
            boolean isSuperuser,
            String permissionId
    ) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.isSuperuser = isSuperuser;
        this.permissionId = permissionId;

        this.id = null;
        this.createdAt = null;
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
        return permissionId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
