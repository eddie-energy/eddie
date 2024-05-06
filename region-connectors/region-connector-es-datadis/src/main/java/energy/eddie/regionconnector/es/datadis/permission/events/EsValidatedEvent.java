package energy.eddie.regionconnector.es.datadis.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class EsValidatedEvent extends PersistablePermissionEvent {
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    public EsValidatedEvent(String permissionId, LocalDate start, LocalDate end, Granularity granularity) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
    }

    protected EsValidatedEvent() {
        super();
        start = null;
        end = null;
        granularity = null;
    }

    public LocalDate end() {
        return end;
    }
}
