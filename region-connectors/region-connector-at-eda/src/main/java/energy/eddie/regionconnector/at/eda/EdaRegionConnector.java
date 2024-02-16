package energy.eddie.regionconnector.at.eda;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.*;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.services.PermissionRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static java.util.Objects.requireNonNull;

@Component
public class EdaRegionConnector implements RegionConnector, Mvp1ConnectionStatusMessageProvider {
    /**
     * DSOs in Austria are only allowed to store data for the last 36 months
     */
    public static final int MAXIMUM_MONTHS_IN_THE_PAST = 36;
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaRegionConnector.class);
    private final EdaAdapter edaAdapter;
    private final PermissionRequestService permissionRequestService;

    /**
     * Used to send permission state messages.
     */
    private final Sinks.Many<ConnectionStatusMessage> permissionStateMessages;

    @Autowired
    public EdaRegionConnector(
            EdaAdapter edaAdapter,
            PermissionRequestService permissionRequestService,
            Sinks.Many<ConnectionStatusMessage> permissionStateMessages
    ) throws TransmissionException {
        requireNonNull(edaAdapter);
        requireNonNull(permissionRequestService);
        requireNonNull(permissionStateMessages);

        this.edaAdapter = edaAdapter;
        this.permissionRequestService = permissionRequestService;
        this.permissionStateMessages = permissionStateMessages;

        edaAdapter.getCMRequestStatusStream()
                .subscribe(this::processIncomingCmStatusMessages);

        edaAdapter.start();
    }

    private static void transitionPermissionRequest(CMRequestStatus cmRequestStatus, AtPermissionRequest request)
            throws StateTransitionException {
        request.setStateTransitionMessage(cmRequestStatus.getMessage());
        switch (cmRequestStatus.getStatus()) {
            case ACCEPTED -> {
                if (request.meteringPointId().isEmpty()) {
                    cmRequestStatus.getMeteringPoint()
                            .ifPresentOrElse(
                                    request::setMeteringPointId,
                                    () -> {
                                        throw new IllegalStateException("Metering point id is missing in ACCEPTED CMRequestStatus message for CMRequest: " + request.cmRequestId());
                                    }
                            );
                }
                Optional<String> cmConsentId = cmRequestStatus.getCMConsentId();
                if (cmConsentId.isPresent()) {
                    request.setConsentId(cmConsentId.get());
                } else if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Got accept message without consent id for permission request with permission id {}",
                            request.permissionId());
                }
                request.accept();
            }
            case ERROR -> {
                // If the DSO does not exist EDA will respond with an error without sending a received-message.
                // In that case the error message is an implicit received-message.
                if (request.state().status() == PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT) {
                    request.receivedPermissionAdministratorResponse();
                }
                request.invalid();
            }
            case REJECTED -> request.reject();
            case RECEIVED -> {
                // we will also receive a received message if we sent a terminate message to the DSO, so we need to make sure not to change the state in that case
                if (request.state().status() == PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT) {
                    request.receivedPermissionAdministratorResponse();
                }
            }
            default -> {
                // Other CMRequestStatus do not change the state of the permission request,
                // because they have no matching state in the consent process model
            }
        }
    }

    @Override
    public Flow.Publisher<ConnectionStatusMessage> getConnectionStatusMessageStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(permissionStateMessages.asFlux());
    }

    @Override
    public RegionConnectorMetadata getMetadata() {
        return EdaRegionConnectorMetadata.getInstance();
    }

    @Override
    public Map<String, HealthState> health() {
        return edaAdapter.health();
    }

    @Override
    public void close() throws Exception {
        edaAdapter.close();
        permissionStateMessages.tryEmitComplete();
    }

    @Override
    public void terminatePermission(String permissionId) {
        LOGGER.info("{} got termination request for permission {}", REGION_CONNECTOR_ID, permissionId);
        var request = permissionRequestService.findByPermissionId(permissionId);
        if (request.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", permissionId);
            return;
        }
        try {
            request.get().terminate();
        } catch (StateTransitionException e) {
            LOGGER.warn("Unexpected exception occured while terminating", e);
        }
    }

    /**
     * Process a CMRequestStatus and emit a ConnectionStatusMessage if possible,
     * also adds connectionId and permissionId for identification
     *
     * @param cmRequestStatus the CMRequestStatus to process
     */
    private void processIncomingCmStatusMessages(CMRequestStatus cmRequestStatus) {
        var optionalPermissionRequest = permissionRequestService.findByConversationIdOrCMRequestId(
                cmRequestStatus.getConversationId(),
                cmRequestStatus.getCMRequestId().orElse(null)
        );
        if (optionalPermissionRequest.isEmpty()) {
            // should not happen if a persistent mapping is used, or if an invalid termination request was sent.
            LOGGER.warn("Received CMRequestStatus for unknown conversationId {} or requestId {} with payload: {}",
                    cmRequestStatus.getConversationId(), cmRequestStatus.getCMRequestId(), cmRequestStatus);
            return;
        }
        try {
            transitionPermissionRequest(cmRequestStatus, optionalPermissionRequest.get());
        } catch (IllegalStateException | StateTransitionException e) {
            permissionStateMessages.tryEmitError(e);
        }
    }
}