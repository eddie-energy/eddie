// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.repositories;

import energy.eddie.cim.agnostic.DataSourceInformation;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
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
           "p.dataNeedType = :dataNeedType AND " +
           "p.permissionAdministratorId = :#{#dsi.permissionAdministratorId()} AND " +
           "p.regionConnectorId = :#{#dsi.regionConnectorId()} AND " +
           "p.countryCode = :#{#dsi.countryCode()}")
    Optional<PermissionRequestMetricsModel> getPermissionRequestMetrics(
            @Param("status") PermissionProcessStatus status,
            @Param("dataNeedType") String dataNeedType,
            @Param("dsi") DataSourceInformation dsi
    );

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO metric.permission_request_metrics (mean, median, permission_request_count, permission_request_status,
                data_need_type, permission_administrator_id, region_connector_id, country_code)
                    VALUES (:#{#model.mean}, :#{#model.median}, :#{#model.permissionRequestCount}, :#{#model.permissionRequestStatus.name()},
                        :#{#model.dataNeedType}, :#{#model.permissionAdministratorId}, :#{#model.regionConnectorId}, :#{#model.countryCode})
                    ON CONFLICT (permission_request_status, data_need_type, permission_administrator_id, region_connector_id, country_code)
                    DO UPDATE SET
                        mean = excluded.mean,
                        median = excluded.median,
                        permission_request_count = excluded.permission_request_count
            """, nativeQuery = true)
    void upsertPermissionRequestMetric(@Param("model") PermissionRequestMetricsModel model);
}
