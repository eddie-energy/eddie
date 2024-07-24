package energy.eddie.regionconnector.at.eda.persistence;

import energy.eddie.api.v0.PermissionProcessStatus;
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
import java.util.Set;

@Repository
@Transactional(readOnly = true)
public interface JpaPermissionRequestRepository extends PagingAndSortingRepository<EdaPermissionRequest, Long>, AtPermissionRequestRepository {

    @Override
    Optional<AtPermissionRequest> findByPermissionId(String permissionId);

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.conversationId = :conversationId OR pr.cmRequestId = :cmRequestId")
    List<AtPermissionRequest> findByConversationIdOrCMRequestId(
            @Param("conversationId") String conversationId,
            @Param("cmRequestId") @Nullable String cmRequestId
    );

    @Override
    @Query("SELECT pr FROM EdaPermissionRequest pr WHERE pr.conversationId = :conversationId AND pr.meteringPointId = :meteringPointId")
    Optional<AtPermissionRequest> findByConversationIdAndMeteringPointId(
            @Param("conversationId") String conversationId,
            @Param("meteringPointId") @Nullable String meteringPointId
    );

    @Override
    @Query("""
             SELECT pr FROM EdaPermissionRequest pr
             WHERE pr.meteringPointId = :meteringPointId
                 AND (pr.status = energy.eddie.api.v0.PermissionProcessStatus.ACCEPTED
                    OR pr.status = energy.eddie.api.v0.PermissionProcessStatus.FULFILLED
                    OR pr.status = energy.eddie.api.v0.PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
                 AND pr.start <= :date
                 AND (pr.end >= :date OR pr.end IS NULL)
            """)
    List<AtPermissionRequest> findAcceptedAndFulfilledAndSentToPAByMeteringPointIdAndDate(
            @Param("meteringPointId") String meteringPointId,
            @Param("date") LocalDate date
    );

    @Override
    @Query("""
             SELECT pr FROM EdaPermissionRequest pr
             WHERE pr.meteringPointId = :meteringPointId
                 AND (pr.status = energy.eddie.api.v0.PermissionProcessStatus.ACCEPTED
                    OR pr.status = energy.eddie.api.v0.PermissionProcessStatus.FULFILLED)
                 AND pr.start <= :date
                 AND (pr.end >= :date OR pr.end IS NULL)
            """)
    List<AtPermissionRequest> findAcceptedAndFulfilledByMeteringPointIdAndDate(
            @Param("meteringPointId") String meteringPointId,
            @Param("date") LocalDate date
    );

    @Override
    Optional<AtPermissionRequest> findByConsentId(@Param("consentId") String consentId);

    @Override
    List<AtPermissionRequest> findByStatusIn(Set<PermissionProcessStatus> status);
}
