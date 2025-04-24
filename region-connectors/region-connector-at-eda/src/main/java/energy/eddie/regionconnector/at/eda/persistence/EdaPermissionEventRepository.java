package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.PersistablePermissionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EdaPermissionEventRepository extends JpaRepository<PersistablePermissionEvent, Long>, PermissionEventRepository {
}