package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "AiidaPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID, name = "permission_event")
@SuppressWarnings("NullAway.Init") // Needed for JPA
public abstract class PersistablePermissionEvent implements PermissionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("UnusedVariable")
    private Long id;

    // Aggregate ID
    private String permissionId;
    private ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private PermissionProcessStatus status;

    protected PersistablePermissionEvent() {}

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
    }

    @Override
    public String permissionId() {
        return permissionId;
    }

    @Override
    public ZonedDateTime eventCreated() {
        return eventCreated;
    }

    @Override
    public PermissionProcessStatus status() {
        return status;
    }
}
