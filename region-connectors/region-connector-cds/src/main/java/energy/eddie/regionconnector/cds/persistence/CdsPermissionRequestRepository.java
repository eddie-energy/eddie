package energy.eddie.regionconnector.cds.persistence;

import energy.eddie.api.agnostic.process.model.persistence.FullPermissionRequestRepository;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CdsPermissionRequestRepository
        extends FullPermissionRequestRepository<CdsPermissionRequest>,
        org.springframework.data.repository.Repository<CdsPermissionRequest, String> {
    Optional<CdsPermissionRequest> findByState(String state);

    @Override
    @Query(
            value = """
                    SELECT *
                    FROM cds.permission_request WHERE status = 'SENT_TO_PERMISSION_ADMINISTRATOR' AND (created <= NOW() - :hours * INTERVAL '1 hour' OR auth_expires_at <= NOW())
                    """,
            nativeQuery = true
    )
    Collection<CdsPermissionRequest> findStalePermissionRequests(@Param("hours") int stalenessDuration);
}
