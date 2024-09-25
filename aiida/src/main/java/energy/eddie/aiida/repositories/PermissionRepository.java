package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    /**
     * Returns all permission for the given userId, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is the first item.
     */
    List<Permission> findByUserIdOrderByGrantTimeDesc(UUID userId);

    /**
     * Returns a list of all active permission, i.e. all permissions, whose status is either {@link PermissionStatus#ACCEPTED} or {@link PermissionStatus#WAITING_FOR_START} or {@link PermissionStatus#STREAMING_DATA}.
     *
     * @return A list of permissions that have the status {@link PermissionStatus#ACCEPTED} or {@link PermissionStatus#WAITING_FOR_START} or {@link PermissionStatus#STREAMING_DATA}.
     */
    @Query("SELECT p FROM Permission p WHERE p.status IN (energy.eddie.aiida.models.permission.PermissionStatus.WAITING_FOR_START, energy.eddie.aiida.models.permission.PermissionStatus.STREAMING_DATA)")
    List<Permission> findAllActivePermissions();
}
