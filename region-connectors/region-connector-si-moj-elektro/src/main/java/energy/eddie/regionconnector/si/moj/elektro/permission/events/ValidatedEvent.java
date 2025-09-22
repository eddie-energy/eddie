package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity(name = "SiValidatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class ValidatedEvent extends PersistablePermissionEvent {

    @Column(name = "permission_start")
    private final LocalDate start;

    @Column(name = "permission_end")
    private final LocalDate end;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final Granularity granularity;

    private final String apiToken;

    public ValidatedEvent(
            String permissionId,
            LocalDate start,
            LocalDate end,
            Granularity granularity,
            String apiToken
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.start = start;
        this.end = end;
        this.granularity = granularity;
        this.apiToken = apiToken;
    }

    protected ValidatedEvent() {
        this.start = null;
        this.end = null;
        this.granularity = null;
        this.apiToken = null;
    }

    public LocalDate start() {
        return start;
    }

    public LocalDate end() {
        return end;
    }

    public Granularity granularity() {
        return granularity;
    }

    public String apiToken() {
        return apiToken;
    }
}
