package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Entity(name = "DeEtaPersistablePermissionEvent")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@Table(schema = "de_eta", name = "permission_event")
@SuppressWarnings("NullAway")
public abstract class PersistablePermissionEvent implements PermissionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @SuppressWarnings("UnusedVariable")
    private final Long id;

    @Column(length = 36)
    private final String permissionId;
    private final ZonedDateTime eventCreated;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final PermissionProcessStatus status;

    // Additional event sourcing columns to match CDS conventions
    @Column(name = "connection_id")
    protected final String connectionId;

    @Column(name = "data_need_id", length = 36)
    protected final String dataNeedId;

    @Column(name = "data_start")
    protected final LocalDate dataStart;

    @Column(name = "data_end")
    protected final LocalDate dataEnd;

    @Column(name = "granularity")
    protected final String granularity;

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
        this.connectionId = null;
        this.dataNeedId = null;
        this.dataStart = null;
        this.dataEnd = null;
        this.granularity = null;
    }

    protected PersistablePermissionEvent(String permissionId, PermissionProcessStatus status, ZonedDateTime created) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = created;
        this.status = status;
        this.connectionId = null;
        this.dataNeedId = null;
        this.dataStart = null;
        this.dataEnd = null;
        this.granularity = null;
    }

    // Convenience constructor to initialize common creation fields
    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status,
            String connectionId,
            String dataNeedId
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataStart = null;
        this.dataEnd = null;
        this.granularity = null;
    }

    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status,
            String connectionId,
            String dataNeedId,
            ZonedDateTime created
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = created;
        this.status = status;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataStart = null;
        this.dataEnd = null;
        this.granularity = null;
    }

    // Constructor for events that carry calculated timeframe and granularity
    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status,
            String connectionId,
            String dataNeedId,
            LocalDate dataStart,
            LocalDate dataEnd,
            String granularity
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = ZonedDateTime.now(ZoneOffset.UTC);
        this.status = status;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        this.granularity = granularity;
    }

    @SuppressWarnings({"java:S107", "ParameterNumber", "checkstyle:ParameterNumber"})
    protected PersistablePermissionEvent(
            String permissionId,
            PermissionProcessStatus status,
            String connectionId,
            String dataNeedId,
            LocalDate dataStart,
            LocalDate dataEnd,
            String granularity,
            ZonedDateTime created
    ) {
        this.id = null;
        this.permissionId = permissionId;
        this.eventCreated = created;
        this.status = status;
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.dataStart = dataStart;
        this.dataEnd = dataEnd;
        this.granularity = granularity;
    }

    protected PersistablePermissionEvent() {
        super();
        this.id = null;
        this.permissionId = null;
        this.eventCreated = null;
        this.status = null;
        this.connectionId = null;
        this.dataNeedId = null;
        this.dataStart = null;
        this.dataEnd = null;
        this.granularity = null;
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
