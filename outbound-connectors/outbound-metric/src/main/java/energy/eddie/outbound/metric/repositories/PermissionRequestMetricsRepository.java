package energy.eddie.outbound.metric.repositories;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.outbound.metric.model.PermissionRequestMetricsModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface PermissionRequestMetricsRepository extends JpaRepository<PermissionRequestMetricsModel, Long> {
    @Query("SELECT p FROM PermissionRequestMetricsModel p WHERE " +
            "p.permissionRequestStatus = :status AND " +
            "p.dataNeedId = :dataNeedId AND " +
            "p.permissionAdministratorId = :permissionAdminId AND " +
            "p.regionConnectorId = :regionConnectorId AND " +
            "p.countryCode = :countryCode")
    Optional<PermissionRequestMetricsModel> getPermissionRequestMetrics(
            @Param("status") PermissionProcessStatus status,
            @Param("dataNeedId") String dataNeedId,
            @Param("permissionAdminId") String permissionAdminId,
            @Param("regionConnectorId") String regionConnectorId,
            @Param("countryCode") String countryCode);

    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO metric.permission_request_metrics (mean, median, permission_request_count, permission_request_status,
        data_need_id, permission_administrator_id, region_connector_id, country_code)
    VALUES (:mean, :median, :permissionRequestCount, :permissionRequestStatus, :dataNeedId, :permissionAdministratorId,
        :regionConnectorId, :countryCode)
    ON CONFLICT (permission_request_status, data_need_id, permission_administrator_id, region_connector_id, country_code)
    DO UPDATE SET 
        mean = EXCLUDED.mean,
        median = EXCLUDED.median,
        permission_request_count = EXCLUDED.permission_request_count
    """, nativeQuery = true)
    void upsertPermissionRequestMetric(@Param("mean") double mean,
                                       @Param("median") double median,
                                       @Param("permissionRequestCount") int permissionRequestCount,
                                       @Param("permissionRequestStatus") String permissionRequestStatus,
                                       @Param("dataNeedId") String dataNeedId,
                                       @Param("permissionAdministratorId") String permissionAdministratorId,
                                       @Param("regionConnectorId") String regionConnectorId,
                                       @Param("countryCode") String countryCode);
}
