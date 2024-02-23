package energy.eddie.regionconnector.es.datadis.permission.request.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.DatadisPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Should only be used by {@link DatadisPermissionRequestRepository}. Other classes should use {@link DatadisPermissionRequestRepository}.
 */
@Repository
public interface JpaPermissionRequestRepository extends JpaRepository<DatadisPermissionRequest, String> {
    List<DatadisPermissionRequest> findAllByStatusIs(PermissionProcessStatus status);
}
