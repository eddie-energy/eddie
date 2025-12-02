package energy.eddie.regionconnector.de.eta.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.ZonedDateTime;

@Entity(name = "DeEtaUnableToSendEvent")
@SuppressWarnings({"NullAway", "unused"})
public class UnableToSendEvent extends PersistablePermissionEvent {

    @Column(name = "reason", columnDefinition = "text")
    private final String reason;

    public UnableToSendEvent(String permissionId,
                             String connectionId,
                             String dataNeedId,
                             String reason) {
        super(permissionId, PermissionProcessStatus.UNABLE_TO_SEND, connectionId, dataNeedId);
        this.reason = truncate(reason);
    }

    public UnableToSendEvent(String permissionId,
                             String connectionId,
                             String dataNeedId,
                             String reason,
                             ZonedDateTime created) {
        super(permissionId, PermissionProcessStatus.UNABLE_TO_SEND, connectionId, dataNeedId, created);
        this.reason = truncate(reason);
    }

    protected UnableToSendEvent() {
        super();
        this.reason = null;
    }

    public String reason() {
        return reason;
    }

    private static String truncate(String input) {
        if (input == null) return null;
        // Limit reason to 1000 characters to avoid oversized payloads
        return input.length() > 1000 ? input.substring(0, 1000) : input;
    }
}
