package energy.eddie.regionconnector.fr.enedis.permission.request.repositories;

import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FutureDataPermissionRepository extends JpaRepository<FutureDataPermission, Integer> {
    List<FutureDataPermission> findAllByValidToAfterAndStateEquals(Instant validTo, String state);

    List<FutureDataPermission> findAllByValidToBeforeOrStateIsNot(Instant validTo, String state);

    FutureDataPermission findFutureDataPermissionByPermissionId(String permissionId);
}
