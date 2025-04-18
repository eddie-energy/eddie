package energy.eddie.regionconnector.at.api;

import energy.eddie.api.agnostic.process.model.persistence.PermissionRequestRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The repository for PermissionRequests. It saves permission requests and can query with the permission id.
 */
public interface AtPermissionRequestRepository extends PermissionRequestRepository<AtPermissionRequest> {

    /**
     * Finds permission requests by either conversation id or CMRequest id. Might return multiple as receiving
     * permission for multiple metering points create new permission request that share the conversation and CMRequest
     * id.
     *
     * @param conversationId the conversation id of the request.
     * @param cmRequestId    the request id of the original CM Request.
     * @return a List, which is empty if there is no matching permission request.
     */
    List<AtPermissionRequest> findByConversationIdOrCMRequestId(
            String conversationId,
            @Nullable String cmRequestId
    );

    /**
     * Finds a permission request by its conversation id and metering point id.
     *
     * @param conversationId  the conversation id of the request.
     * @param meteringPointId the metering point id of the request.
     * @return an optional, which is empty if there is no matching permission request.
     */
    Optional<AtPermissionRequest> findByConversationIdAndMeteringPointId(
            String conversationId,
            String meteringPointId
    );

    /**
     * This method returns all {@link energy.eddie.api.v0.PermissionProcessStatus#ACCEPTED} and
     * {@link energy.eddie.api.v0.PermissionProcessStatus#FULFILLED} permission requests that are associated with the
     * given metering point and where the date is between start and end of the permission request
     *
     * @param meteringPointId for which to get permission requests
     * @param date            to filter time relevant permission requests
     * @return a list of matching permission requests
     */
    List<AtPermissionRequest> findAcceptedAndFulfilledByMeteringPointIdAndDate(
            String meteringPointId,
            LocalDate date
    );

    Optional<AtPermissionRequest> findByConsentId(String consentId);

    List<AtPermissionRequest> findByStatusIn(Set<PermissionProcessStatus> status);
}
