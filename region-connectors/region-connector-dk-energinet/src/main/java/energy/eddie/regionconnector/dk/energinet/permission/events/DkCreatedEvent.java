package energy.eddie.regionconnector.dk.energinet.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class DkCreatedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String meteringPointId;
    @Column(columnDefinition = "text")
    private final String refreshToken;

    public DkCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String meteringPointId,
            String refreshToken
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.meteringPointId = meteringPointId;
        this.refreshToken = refreshToken;
    }

    protected DkCreatedEvent() {
        this.connectionId = null;
        this.dataNeedId = null;
        this.meteringPointId = null;
        this.refreshToken = null;
    }

    public String connectionId() {
        return connectionId;
    }

    public String dataNeedId() {
        return dataNeedId;
    }

    public String meteringPointId() {
        return meteringPointId;
    }

    public String refreshToken() {
        return refreshToken;
    }
}
