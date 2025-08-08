package energy.eddie.outbound.metric.model;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "permission_request_status_duration", schema = "metric", uniqueConstraints = @UniqueConstraint(
        columnNames = {"permission_id", "permission_request_status"})
)
@SuppressWarnings("NullAway")
public class PermissionRequestStatusDurationModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "permission_id", nullable = false, updatable = false)
    private String permissionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_request_status", nullable = false, updatable = false)
    private PermissionProcessStatus status;

    @Column(name = "duration_milliseconds", nullable = false, updatable = false)
    private long duration;

    @Column(name = "data_need_type", nullable = false, updatable = false)
    private String dataNeedType;

    @Column(name = "permission_administrator_id", nullable = false, updatable = false)
    private String permissionAdministratorId;

    @Column(name = "region_connector_id", nullable = false, updatable = false)
    private String regionConnectorId;

    @Column(name = "country_code", nullable = false, updatable = false)
    private String countryCode;

    protected PermissionRequestStatusDurationModel() { }

    public PermissionRequestStatusDurationModel(String permissionId, PermissionProcessStatus status, long duration,
            String dataNeedType, String permissionAdministratorId, String regionConnectorId, String countryCode) {
        this.permissionId = permissionId;
        this.status = status;
        this.duration = duration;
        this.dataNeedType = dataNeedType;
        this.permissionAdministratorId = permissionAdministratorId;
        this.regionConnectorId = regionConnectorId;
        this.countryCode = countryCode;
    }

    public Long getId() {
        return id;
    }

    public String getPermissionId() {
        return permissionId;
    }

    public PermissionProcessStatus getStatus() {
        return status;
    }

    public long getDuration() {
        return duration;
    }

    public String getDataNeedType() {
        return dataNeedType;
    }

    public String getPermissionAdministratorId() {
        return permissionAdministratorId;
    }

    public String getRegionConnectorId() {
        return regionConnectorId;
    }

    public String getCountryCode() {
        return countryCode;
    }
}
