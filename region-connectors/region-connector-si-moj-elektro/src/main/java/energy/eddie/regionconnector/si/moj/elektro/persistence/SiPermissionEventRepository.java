package energy.eddie.regionconnector.si.moj.elektro.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.si.moj.elektro.permission.events.PersistablePermissionEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SiPermissionEventRepository extends PermissionEventRepository, CrudRepository<PersistablePermissionEvent, Long> {
}
