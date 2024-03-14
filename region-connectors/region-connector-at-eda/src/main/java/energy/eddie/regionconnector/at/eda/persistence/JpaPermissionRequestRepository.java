package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import jakarta.annotation.Nullable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public interface JpaPermissionRequestRepository extends PagingAndSortingRepository<EdaPermissionRequest, Long>, AtPermissionRequestRepository {

    @Override
    Optional<AtPermissionRequest> findByPermissionId(String permissionId);

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.conversationId = :conversationId OR pr.cmRequestId = :cmRequestId")
    Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(@Param("conversationId") String conversationId,
                                                                    @Param("cmRequestId") @Nullable String cmRequestId);

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.meteringPointId = :meteringPointId AND cast(pr.start as date) < :date AND (cast(pr.end as date) > :date OR pr.end IS NULL)")
    List<AtPermissionRequest> findByMeteringPointIdAndDate(@Param("meteringPointId") String meteringPointId,
                                                           @Param("date") LocalDate date);

    @Override
    Optional<AtPermissionRequest> findByConsentId(@Param("consentId") String consentId);

}
