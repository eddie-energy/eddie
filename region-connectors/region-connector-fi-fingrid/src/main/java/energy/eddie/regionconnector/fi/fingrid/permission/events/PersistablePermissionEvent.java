package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.Clock;
import java.time.ZonedDateTime;

@Entity(name = "FiPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "fi_fingrid", name = "permission_event")
@SuppressWarnings("NullAway") // Needed for JPA
public abstract class PersistablePermissionEvent implements PermissionEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("unused")
    private final Long id;

    // Aggregate ID
    @Column(length = 36)
    private final String permissionId;
    private final ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this(permissionId, status, Clock.systemUTC());
    }

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status, Clock clock) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(clock);
        this.status = status;
    }

    protected PersistablePermissionEvent() {
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
    public PermissionProcessStatus status() {
        return status;
    }

    @Override
    public ZonedDateTime eventCreated() {
        return eventCreated;
    }
}
