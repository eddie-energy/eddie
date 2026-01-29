package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import javax.annotation.Nullable;
import java.time.LocalDate;

/**
 * Event emitted when a permission request is validated.
 */
@Entity(name = "DeValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class ValidatedEvent extends PersistablePermissionEvent {

    @Column(name = "data_start")
    private LocalDate start;
    @Column(name = "data_end")
    private LocalDate end;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private Granularity granularity;

    public ValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            @Nullable Granularity granularity
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
    }

    protected ValidatedEvent() { }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public Granularity granularity() {
        return granularity;
    }
}
