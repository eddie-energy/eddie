// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EsPermissionRequestRepository extends
        JpaRepository<DatadisPermissionRequest, String>,
        StatusPermissionRequestRepository<EsPermissionRequest>,
        StalePermissionRequestRepository<DatadisPermissionRequest>,
        PermissionRequestRepository<EsPermissionRequest> {

    @Override
    List<EsPermissionRequest> findByStatus(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, nif, metering_point_id, permission_start, permission_end, data_need_id, granularity, allowed_granularity, distributor_code, point_type, latest_meter_reading, status, error_message, production_support, created, bundle_id FROM es_datadis.datadis_permission_request " +
                    "WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<DatadisPermissionRequest> findStalePermissionRequests(@Param("hours") int timeoutDuration);

    @Query(
            value = """
                        WITH permissions AS (
                            SELECT DISTINCT ON (permission_id) permission_id
                            FROM es_datadis.permission_event
                            WHERE bundle_id = :bundleId
                        )
                        SELECT DISTINCT ON (permission_id) permission_id,
                                                           es_datadis.firstval_agg(connection_id) OVER w        AS connection_id,
                                                           es_datadis.firstval_agg(nif) OVER w                  AS nif,
                                                           es_datadis.firstval_agg(metering_point_id) OVER w    AS metering_point_id,
                                                           es_datadis.firstval_agg(permission_start) OVER w     AS permission_start,
                                                           es_datadis.firstval_agg(permission_end) OVER w       AS permission_end,
                                                           es_datadis.firstval_agg(data_need_id) OVER w         AS data_need_id,
                                                           es_datadis.firstval_agg(granularity) OVER w          AS granularity,
                                                           es_datadis.firstval_agg(allowed_granularity) OVER w  AS allowed_granularity,
                                                           es_datadis.firstval_agg(distributor_code) OVER w     AS distributor_code,
                                                           es_datadis.firstval_agg(supply_point_type) OVER w    AS point_type,
                                                           es_datadis.firstval_agg(latest_meter_reading) OVER w AS latest_meter_reading,
                                                           es_datadis.firstval_agg(status) OVER w               AS status,
                                                           es_datadis.firstval_agg(message) OVER w              AS error_message,
                                                           es_datadis.firstval_agg(production_support) OVER w   AS production_support,
                                                           MIN(event_created) OVER w                            AS created,
                                                           es_datadis.firstval_agg(bundle_id) OVER w            AS bundle_id
                        FROM es_datadis.permission_event
                        JOIN permissions USING (permission_id)
                        WINDOW w AS (
                            PARTITION BY permission_id ORDER BY event_created DESC
                            ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
                        )
                        ORDER BY permission_id, event_created;
                    """,
            nativeQuery = true
    )
    List<DatadisPermissionRequest> findAllByBundleId(@Param("bundleId") UUID bundleId);
}