package energy.eddie.regionconnector.es.datadis.persistence;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EsPermissionRequestRepository extends PermissionRequestRepository<EsPermissionRequest>, JpaRepository<DatadisPermissionRequest, String> {

    List<EsPermissionRequest> findByStatus(PermissionProcessStatus status);
}