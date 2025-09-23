package energy.eddie.regionconnector.si.moj.elektro.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity(name = "SiCreatedEvent")
@SuppressWarnings({"NullAway", "unused"})
public class CreatedEvent extends PersistablePermissionEvent {

    @Column(length = 36)
    private String dataNeedId;
    private String connectionId;
    private String apiToken;
    private String meteringPointId;

    public CreatedEvent(String permissionId,
                        String dataNeedId,
                        String connectionId,
                        String apiToken,
                        String meteringPointId
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.dataNeedId = dataNeedId;
        this.connectionId = connectionId;
        this.apiToken = apiToken;
        this.meteringPointId = meteringPointId;
    }

    protected CreatedEvent() { }

    public String dataNeedId() {
        return dataNeedId;
    }

    public String connectionId() {
        return connectionId;
    }

    public String apiToken() {
        return apiToken;
    }

    public String meteringPointId() {
        return meteringPointId;
    }
}
