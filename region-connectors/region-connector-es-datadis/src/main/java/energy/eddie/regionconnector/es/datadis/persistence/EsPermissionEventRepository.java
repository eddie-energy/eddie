package energy.eddie.regionconnector.es.datadis.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.es.datadis.permission.events.PersistablePermissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsPermissionEventRepository extends JpaRepository<PersistablePermissionEvent, Long>, PermissionEventRepository {
}
