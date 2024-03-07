package energy.eddie.regionconnector.fr.enedis.permission.request.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Should only be used by {@link EnedisPermissionRequestRepository}. Other classes should use {@link EnedisPermissionRequestRepository}.
 */
public interface JpaPermissionRequestRepository extends JpaRepository<EnedisPermissionRequest, String> {
    List<EnedisPermissionRequest> findAllByStatusIs(PermissionProcessStatus status);
}
