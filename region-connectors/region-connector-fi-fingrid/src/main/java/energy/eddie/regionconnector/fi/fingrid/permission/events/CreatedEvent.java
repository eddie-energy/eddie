package energy.eddie.regionconnector.fi.fingrid.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "FiCreatedEvent")
@SuppressWarnings({"unused", "NullAway"}) // Needed for JPA
public class CreatedEvent extends PersistablePermissionEvent {
    @Column(name = "connection_id")
    private final String connectionId;
    @Column(name = "data_need_id", length = 36)
    private final String dataNeedId;
    @Column(length = 50)
    private final String customerIdentification;

    public CreatedEvent(String permissionId, String connectionId, String dataNeedId, String customerIdentification) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.customerIdentification = customerIdentification;
    }

    protected CreatedEvent() {
        connectionId = null;
        dataNeedId = null;
        customerIdentification = null;
    }
}
