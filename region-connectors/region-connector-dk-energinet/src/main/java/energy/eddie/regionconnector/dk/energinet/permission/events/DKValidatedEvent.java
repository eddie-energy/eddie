package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DKValidatedEvent extends PersistablePermissionEvent {
    @Enumerated(EnumType.STRING)
    private final Granularity granularity;
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;

    public DKValidatedEvent(String permissionId, Granularity granularity, LocalDate start, LocalDate end) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.granularity = granularity;
        this.start = start;
        this.end = end;
    }

    protected DKValidatedEvent() {
        granularity = null;
        start = null;
        end = null;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }
}
