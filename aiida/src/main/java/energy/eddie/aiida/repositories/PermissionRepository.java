package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    /**
     * Returns all permission objects stored within the database, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is the first item.
     */
    List<Permission> findAllByOrderByGrantTimeDesc();
}
