package energy.eddie.regionconnector.de.eta.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.de.eta.permission.request.events.PersistablePermissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA repository for permission events in the German (DE) region connector.
 * Extends both JpaRepository for database operations and PermissionEventRepository
 * for EDDIE framework integration.
 */
@Repository
public interface DePermissionEventRepository 
        extends JpaRepository<PersistablePermissionEvent, Long>, PermissionEventRepository {
}
