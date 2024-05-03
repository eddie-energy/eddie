package energy.eddie.regionconnector.dk.energinet.persistence;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.regionconnector.dk.energinet.permission.events.PersistablePermissionEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface DkPermissionEventRepository extends PermissionEventRepository, org.springframework.data.repository.Repository<PersistablePermissionEvent, Long> {
}
