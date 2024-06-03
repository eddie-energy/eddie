package energy.eddie.regionconnector.at.eda.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDate;

@Entity
@SuppressWarnings("NullAway") // Needed for JPA
public class ValidatedEvent extends PersistablePermissionEvent {
    private final LocalDate permissionStart;
    private final LocalDate permissionEnd;
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "text")
    private final AllowedGranularity granularity;
    private final String cmRequestId;
    private final String conversationId;

    public ValidatedEvent() {
        super();
        permissionStart = null;
        permissionEnd = null;
        granularity = null;
        cmRequestId = null;
        conversationId = null;
    }

    public ValidatedEvent(
            String permissionId,
            LocalDate permissionStart,
            @Nullable
            LocalDate permissionEnd,
            @Nullable
            AllowedGranularity granularity,
            String cmRequestId,
            String conversationId
    ) {
        super(permissionId, PermissionProcessStatus.VALIDATED);
        this.permissionStart = permissionStart;
        this.permissionEnd = permissionEnd;
        this.granularity = granularity;
        this.cmRequestId = cmRequestId;
        this.conversationId = conversationId;
    }

    public LocalDate start() {
        return permissionStart;
    }

    @Nullable
    public LocalDate end() {
        return permissionEnd;
    }

    @Nullable
    public AllowedGranularity granularity() {
        return granularity;
    }

    public String cmRequestId() {
        return cmRequestId;
    }

    public String conversationId() {
        return conversationId;
    }
}
