package energy.eddie.regionconnector.aiida.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity(name = "AiidaCreatedEvent")
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id")
    private final String dataNeedId;
    @Column(name = "permission_start")
    private final LocalDate permissionStart;
    @Column(name = "permission_end")
    private final LocalDate permissionEnd;
    @Column(name = "termination_topic")
    private final String terminationTopic;

    @SuppressWarnings("NullAway") // Needed for JPA
    protected CreatedEvent() {
        this.connectionId = null;
        this.dataNeedId = null;
        this.permissionStart = null;
        this.permissionEnd = null;
        this.terminationTopic = null;
    }

    public CreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            LocalDate permissionStart,
            LocalDate permissionEnd,
            String terminationTopic
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
        this.terminationTopic = terminationTopic;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public LocalDate permissionStart() {
        return permissionStart;
    }

    public LocalDate permissionEnd() {
        return permissionEnd;
    }

    public String terminationTopic() {
        return terminationTopic;
    }
}
