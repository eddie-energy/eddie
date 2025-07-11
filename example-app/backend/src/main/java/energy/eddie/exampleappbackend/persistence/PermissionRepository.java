package energy.eddie.exampleappbackend.persistence;

import energy.eddie.exampleappbackend.model.db.Permission;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends CrudRepository<Permission, Long> {
    Optional<Permission> findByEddiePermissionId(String eddiePermissionId);
    List<Permission> findByUserId(String userId);
}
