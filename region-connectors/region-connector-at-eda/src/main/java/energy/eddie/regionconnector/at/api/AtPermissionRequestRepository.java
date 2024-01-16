package energy.eddie.regionconnector.at.api;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The repository for PermissionRequests.
 * It saves permission requests and can query with the permission id.
 */
public interface AtPermissionRequestRepository extends PermissionRequestRepository<AtPermissionRequest> {

    /**
     * Finds a permission request by either its conversation id or its CMRequest id.
     *
     * @param conversationId the conversation id of the request.
     * @param cmRequestId    the request id of the original CM Request.
     * @return an optional, which is empty if there is no matching permission request.
     */
    Optional<AtPermissionRequest> findByConversationIdOrCMRequestId(String conversationId, @Nullable String cmRequestId);

    /**
     * This method returns all permission requests that are associated with the given metering point, and where the date is between start and end of the permission request
     *
     * @param meteringPointId for which to get permission requests
     * @param date            to filter time relevant permission requests
     * @return a list of matching permission requests
     */
    List<AtPermissionRequest> findByMeteringPointIdAndDate(String meteringPointId, LocalDate date);

    Optional<AtPermissionRequest> findByConsentId(String consentId);
}
