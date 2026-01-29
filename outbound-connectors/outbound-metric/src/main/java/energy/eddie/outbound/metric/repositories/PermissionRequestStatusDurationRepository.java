// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.metric.repositories;

import energy.eddie.outbound.metric.model.PermissionRequestStatusDurationModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRequestStatusDurationRepository extends JpaRepository<PermissionRequestStatusDurationModel, Long> {
    @Query(value = """
        SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY duration_milliseconds)
        FROM metric.permission_request_status_duration
        WHERE permission_request_status = :status AND
              data_need_type = :dataNeedType AND
              region_connector_id = :regionConnectorId AND
              permission_administrator_id = :permissionAdministratorId AND
              country_code = :countryCode
        """, nativeQuery = true)
    double getMedianDurationMilliseconds(
            @Param("status") String status,
            @Param("dataNeedType") String dataNeedType,
            @Param("permissionAdministratorId") String permissionAdministratorId,
            @Param("regionConnectorId") String regionConnectorId,
            @Param("countryCode") String countryCode
    );
}
