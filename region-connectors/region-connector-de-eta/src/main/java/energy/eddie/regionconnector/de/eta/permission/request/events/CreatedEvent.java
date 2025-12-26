package energy.eddie.regionconnector.de.eta.permission.request.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

/**
 * Event emitted when a new permission request is created.
 */
@Entity(name = "DeCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {

    @Column(name = "data_need_id_str", length = 36)
    private String dataNeedId;
    
    @Column(name = "connection_id")
    private String connectionId;
    
    @Column(name = "metering_point_id")
    private String meteringPointId;

    public CreatedEvent(String permissionId, String dataNeedId, String connectionId, String meteringPointId) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.dataNeedId = dataNeedId;
        this.connectionId = connectionId;
        this.meteringPointId = meteringPointId;
    }

    protected CreatedEvent() { }

    public String dataNeedId() {
        return dataNeedId;
    }

    public String connectionId() {
        return connectionId;
    }

    public String meteringPointId() {
        return meteringPointId;
    }
}
