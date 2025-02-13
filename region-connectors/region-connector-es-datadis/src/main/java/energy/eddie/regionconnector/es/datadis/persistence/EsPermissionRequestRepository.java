package energy.eddie.regionconnector.es.datadis.persistence;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StalePermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.persistence.StatusPermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EsPermissionRequestRepository extends
        JpaRepository<DatadisPermissionRequest, String>,
        StatusPermissionRequestRepository<EsPermissionRequest>,
        StalePermissionRequestRepository<DatadisPermissionRequest>,
        PermissionRequestRepository<EsPermissionRequest> {

    List<EsPermissionRequest> findByStatus(PermissionProcessStatus status);

    @Query(
            value = "SELECT permission_id, connection_id, nif, metering_point_id, permission_start, permission_end, data_need_id, granularity, allowed_granularity, distributor_code, point_type, latest_meter_reading, status, error_message, production_support, created FROM es_datadis.datadis_permission_request " +
                    "WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND created <= NOW() - :hours * INTERVAL '1 hour'",
            nativeQuery = true
    )
    @Override
    List<DatadisPermissionRequest> findStalePermissionRequests(@Param("hours") int timeoutDuration);
}