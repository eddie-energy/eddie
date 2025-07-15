package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity(name = "FiValidatedEvent")
@SuppressWarnings({"unused", "NullAway"})
public class ValidatedEvent extends PersistablePermissionEvent {
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;

    public ValidatedEvent(String permissionId, Granularity granularity, LocalDate start, LocalDate end) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.granularity = granularity;
        this.start = start;
        this.end = end;
    }

    public ValidatedEvent(String permissionId, LocalDate start, LocalDate end) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.granularity = null;
        this.start = start;
        this.end = end;
    }

    protected ValidatedEvent() {
        this.granularity = null;
        this.start = null;
        this.end = null;
    }
}
