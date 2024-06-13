package energy.eddie.regionconnector.us.green.button.permission.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.us.green.button.permission.request.GreenButtonDataSourceInformation;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;

@Entity
@SuppressWarnings({"NullAway", "unused"})
public class UsCreatedEvent extends PersistablePermissionEvent {
    @Column(columnDefinition = "text")
    private final String connectionId;
    @Column(length = 36)
    private final String dataNeedId;
    @Column(columnDefinition = "text")
    private final String jumpOffUrl;
    @Embedded
    private final GreenButtonDataSourceInformation dataSourceInformation;

    public UsCreatedEvent(
            String permissionId,
            String connectionId,
            String dataNeedId,
            String jumpOffUrl,
            GreenButtonDataSourceInformation dataSourceInformation
    ) {
        super(permissionId, PermissionProcessStatus.CREATED);
        this.connectionId = connectionId;
        this.dataNeedId = dataNeedId;
        this.jumpOffUrl = jumpOffUrl;
        this.dataSourceInformation = dataSourceInformation;
    }

    protected UsCreatedEvent() {
        super();
        connectionId = null;
        dataNeedId = null;
        jumpOffUrl = null;
        dataSourceInformation = null;
    }
}
