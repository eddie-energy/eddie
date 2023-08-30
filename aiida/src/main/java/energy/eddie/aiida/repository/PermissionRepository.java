package energy.eddie.aiida.repository;

import energy.eddie.aiida.model.permission.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, String> {
}
