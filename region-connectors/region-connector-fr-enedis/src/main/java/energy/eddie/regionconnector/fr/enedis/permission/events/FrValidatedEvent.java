package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrValidatedEvent extends PersistablePermissionEvent {
    @Column(name = "permission_start")
    private final LocalDate start;
    @Column(name = "permission_end")
    private final LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    public FrValidatedEvent(String permissionId, LocalDate start, LocalDate end, Granularity granularity) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
    }

    protected FrValidatedEvent() {
        super();
        this.start = null;
        this.end = null;
        this.granularity = null;
    }
}
