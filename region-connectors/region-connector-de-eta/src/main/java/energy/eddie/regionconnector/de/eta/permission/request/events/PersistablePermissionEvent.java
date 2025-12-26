package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Base class for persistable permission events in the German (DE) region connector.
 * This class provides JPA persistence capabilities for permission events.
 */
@Entity(name = "DePersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@Table(schema = "de_eta", name = "permission_event")
@SuppressWarnings("NullAway") // Needed for JPA
public abstract class PersistablePermissionEvent implements PermissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("UnusedVariable")
    private final Long id;

    // Aggregate ID
    @Column(name = "permission_id", length = 36)
    private final String permissionId;
    
    @Column(name = "event_created")
    private final ZonedDateTime eventCreated;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", columnDefinition = "text")
    private final PermissionProcessStatus status;

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
    }

    protected PersistablePermissionEvent() {
        super();
        this.id = null;
        this.permissionId = null;
        this.eventCreated = null;
        this.status = null;
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
