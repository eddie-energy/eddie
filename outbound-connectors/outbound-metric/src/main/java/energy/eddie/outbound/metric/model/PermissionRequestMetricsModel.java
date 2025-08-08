package energy.eddie.outbound.metric.model;

import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "permission_request_metrics", schema = "metric", uniqueConstraints = @UniqueConstraint(
        columnNames = {"permission_request_status", "data_need_type", "permission_administrator_id",
                "region_connector_id", "country_code"})
)
@SuppressWarnings("NullAway")
public class PermissionRequestMetricsModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private double mean;

    @Column(nullable = false)
    private double median;

    @Column(name = "permission_request_count")
    private int permissionRequestCount = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "permission_request_status", nullable = false, updatable = false)
    private PermissionProcessStatus permissionRequestStatus;

    @Column(name = "data_need_type", nullable = false, updatable = false)
    private String dataNeedType;

    @Column(name = "permission_administrator_id", nullable = false, updatable = false)
    private String permissionAdministratorId;

    @Column(name = "region_connector_id", nullable = false, updatable = false)
    private String regionConnectorId;

    @Column(name = "country_code", nullable = false, updatable = false)
    private String countryCode;

    public PermissionRequestMetricsModel(double mean, double median, PermissionProcessStatus permissionRequestStatus,
            String dataNeedType, String permissionAdministratorId, String regionConnectorId, String countryCode) {
        this.mean = mean;
        this.median = median;
        this.permissionRequestStatus = permissionRequestStatus;
        this.dataNeedType = dataNeedType;
        this.permissionAdministratorId = permissionAdministratorId;
        this.regionConnectorId = regionConnectorId;
        this.countryCode = countryCode;
    }

    public PermissionRequestMetricsModel() {

    }

    public Long getId() {
        return id;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public int getPermissionRequestCount() {
        return permissionRequestCount;
    }

    public PermissionProcessStatus getPermissionRequestStatus() {
        return permissionRequestStatus;
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
