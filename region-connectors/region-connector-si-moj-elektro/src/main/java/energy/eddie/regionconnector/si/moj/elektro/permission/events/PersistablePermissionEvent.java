package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "MojElektroPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "si_moj_elektro", name = "permission_event")
@SuppressWarnings("NullAway")
public abstract class PersistablePermissionEvent implements PermissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 36)
    private String permissionId;
    private ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private PermissionProcessStatus status;

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this.permissionId = permissionId;
        this.status = status;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
    }

    protected PersistablePermissionEvent() { }

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
