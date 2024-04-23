package energy.eddie.regionconnector.fr.enedis.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.fr.enedis.permission.events.PersistablePermissionEvent;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FrPermissionEventRepository extends PermissionEventRepository, CrudRepository<PersistablePermissionEvent, Long> {
}
