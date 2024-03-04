package energy.eddie.regionconnector.fr.enedis.permission.request.persistence;

import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Should only be used by {@link EnedisPermissionRequestRepository}. Other classes should use {@link EnedisPermissionRequestRepository}.
 */
public interface JpaPermissionRequestRepository extends JpaRepository<EnedisPermissionRequest, String> {
}
