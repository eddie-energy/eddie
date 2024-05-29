package energy.eddie.regionconnector.fr.enedis.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import java.time.Clock;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class FrCreatedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;

    public FrCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
    }

    public FrCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            Clock clock
    ) {
        super(permissionId, PermissionProcessStatus.CREATED, clock);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
    }

    protected FrCreatedEvent() {
        super();
        connectionId = null;
        dataNeedId = null;
    }
}
