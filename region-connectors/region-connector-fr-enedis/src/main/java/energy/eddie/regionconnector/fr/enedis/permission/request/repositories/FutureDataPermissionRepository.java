package energy.eddie.regionconnector.fr.enedis.permission.request.repositories;

import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FutureDataPermissionRepository extends JpaRepository<FutureDataPermission, Integer> {
    @Query("select fdp from FutureDataPermission fdp where fdp.state = ?2 AND fdp.validFrom < ?1 AND (fdp.validTo > ?1 OR fdp.validTo IS NULL) AND (fdp.lastPoll <?1 OR fdp.lastPoll IS NULL)")
    List<FutureDataPermission> findValidFutureDataPermissions(Instant today, String state);

    @Query("select fdp from FutureDataPermission fdp where fdp.state <> ?2 OR fdp.validTo < ?1")
    List<FutureDataPermission> findInvalidFutureDataPermissions(Instant validTo, String state);

    FutureDataPermission findFutureDataPermissionByPermissionId(String permissionId);
}
