package energy.eddie.regionconnector.be.fluvius.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.be.fluvius.permission.events.PersistablePermissionEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BePermissionEventRepository extends PermissionEventRepository, CrudRepository<PersistablePermissionEvent, Long> {
}
