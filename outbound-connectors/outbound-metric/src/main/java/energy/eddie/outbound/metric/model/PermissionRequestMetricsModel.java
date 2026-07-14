// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.model;

import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.persistence.*;

@Entity
@Table(name = "permission_request_metrics",
        schema = "metric",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "permission_request_status",
                        "data_need_type",
                        "permission_administrator_id",
                        "region_connector_id",
                        "country_code"
                })
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

    public PermissionRequestMetricsModel(
            double mean,
            double median,
            PermissionProcessStatus permissionRequestStatus,
            String dataNeedType,
            DataSourceInformation dataSourceInformation
    ) {
        this.mean = mean;
        this.median = median;
        this.permissionRequestStatus = permissionRequestStatus;
        this.dataNeedType = dataNeedType;
        this.permissionAdministratorId = dataSourceInformation.permissionAdministratorId();
        this.regionConnectorId = dataSourceInformation.regionConnectorId();
        this.countryCode = dataSourceInformation.countryCode();
    }

    public PermissionRequestMetricsModel(
            double mean,
            double median,
            int count,
            PermissionProcessStatus prevEventStatus,
            String dataNeedType,
            DataSourceInformation dataSourceInformation
    ) {
        this.mean = mean;
        this.median = median;
        this.permissionRequestCount = count;
        this.permissionRequestStatus = prevEventStatus;
        this.dataNeedType = dataNeedType;
        this.permissionAdministratorId = dataSourceInformation.permissionAdministratorId();
        this.regionConnectorId = dataSourceInformation.regionConnectorId();
        this.countryCode = dataSourceInformation.countryCode();
    }

    protected PermissionRequestMetricsModel() {}

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

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Double.hashCode(mean);
        result = 31 * result + Double.hashCode(median);
        result = 31 * result + permissionRequestCount;
        result = 31 * result + permissionRequestStatus.hashCode();
        result = 31 * result + dataNeedType.hashCode();
        result = 31 * result + permissionAdministratorId.hashCode();
        result = 31 * result + regionConnectorId.hashCode();
        result = 31 * result + countryCode.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PermissionRequestMetricsModel that)) return false;

        return Double.compare(mean, that.mean) == 0
               && Double.compare(median, that.median) == 0
               && permissionRequestCount == that.permissionRequestCount
               && id.equals(that.id)
               && permissionRequestStatus == that.permissionRequestStatus
               && dataNeedType.equals(that.dataNeedType)
               && permissionAdministratorId.equals(that.permissionAdministratorId)
               && regionConnectorId.equals(that.regionConnectorId)
               && countryCode.equals(that.countryCode);
    }

    @Override
    public String toString() {
        return "PermissionRequestMetricsModel{" +
               "id=" + id +
               ", mean=" + mean +
               ", median=" + median +
               ", permissionRequestCount=" + permissionRequestCount +
               ", permissionRequestStatus=" + permissionRequestStatus +
               ", dataNeedType='" + dataNeedType + '\'' +
               ", permissionAdministratorId='" + permissionAdministratorId + '\'' +
               ", regionConnectorId='" + regionConnectorId + '\'' +
               ", countryCode='" + countryCode + '\'' +
               '}';
    }
}
