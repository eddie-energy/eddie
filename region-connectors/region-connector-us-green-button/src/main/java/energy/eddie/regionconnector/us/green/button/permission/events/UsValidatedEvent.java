package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class UsValidatedEvent extends PersistablePermissionEvent {
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;
    @Column(columnDefinition = "text")
    private final String scope;

    public UsValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            String scope
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.scope = scope;
    }

    public UsValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            String scope
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = null;
        this.scope = scope;
    }

    protected UsValidatedEvent() {
        super();
        this.start = null;
        this.end = null;
        this.granularity = null;
        this.scope = null;
    }
}
