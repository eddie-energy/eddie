// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.permission.request.EdaDataSourceInformation;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.ValidatedEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CCMOAcceptHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CCMOAcceptHandler.class);
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    public CCMOAcceptHandler(AtPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    public void handleCCMOAccept(CMRequestStatus cmRequestStatus) {
        var permissionRequests = repository.findByConversationIdOrCMRequestId(
                cmRequestStatus.conversationId(),
                cmRequestStatus.cmRequestId()
        );

        if (permissionRequests.isEmpty()) {
            // should not happen if a persistent mapping is used.
            LOGGER.error("Received CCMO_ACCEPT for unknown conversationId '{}' or requestId '{}' with payload: '{}'",
                         cmRequestStatus.conversationId(), cmRequestStatus.cmRequestId(), cmRequestStatus);
            return;
        }
        if (permissionRequests.size() > 1) {
            // This should never happen, since before receiving the CCMO_ACCEPT there should be only one permission request, as this is the code responsible for creating new permission requests
            // Having multiple permission requests here indicates a resent CCMO_ACCEPT message, which should only happen in manual resending of messages
            LOGGER.atError()
                  .addArgument(cmRequestStatus::conversationId)
                  .addArgument(cmRequestStatus::cmRequestId)
                  .log("Found multiple matching permission requests for conversationId '{}' or requestId '{}'. This should never happen at this stage, only considering the first.");
        }

        var permissionRequest = permissionRequests.stream()
                                                  .map(EdaPermissionRequest::fromProjection)
                                                  .toList().getFirst();

        var status = permissionRequest.status();
        if (status == PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR) {
            acceptPermissionRequest(cmRequestStatus.consentData().getFirst(), permissionRequest);
        } else {
            LOGGER.atInfo()
                  .addArgument(status)
                  .addArgument(permissionRequest::permissionId)
                  .log("Got acceptance message for {} permission request {}, ignoring status change");
        }
        for (var consentData : cmRequestStatus.consentData().subList(1, cmRequestStatus.consentData().size())) {
            createNewPermissionRequest(consentData, permissionRequest);
        }
    }

    private void acceptPermissionRequest(
            ConsentData consentData,
            AtPermissionRequest permissionRequest
    ) {
        var consentId = consentData.cmConsentId();
        if (consentId.isEmpty()) {
            LOGGER.atWarn()
                  .addArgument(permissionRequest::permissionId)
                  .log("Consent ID is missing in ACCEPTED CMRequestStatus message for permission request {}");
            return;
        }
        var meteringPoint = consentData.meteringPoint();
        if (meteringPoint.isEmpty()) {
            LOGGER.atWarn()
                  .addArgument(permissionRequest::permissionId)
                  .log("Metering point id is missing in ACCEPTED CMRequestStatus message for permission request {}");
            return;
        }
        outbox.commit(new AcceptedEvent(
                permissionRequest.permissionId(),
                meteringPoint.get(),
                consentId.get(),
                consentData.message()
        ));
    }

    private void createNewPermissionRequest(ConsentData consentData, AtPermissionRequest permissionRequest) {
        var permissionId = UUID.randomUUID().toString();
        outbox.commit(new CreatedEvent(
                permissionId,
                permissionRequest.connectionId(),
                permissionRequest.dataNeedId(),
                permissionRequest.created(),
                new EdaDataSourceInformation(permissionRequest.dataSourceInformation().meteredDataAdministratorId()),
                consentData.meteringPoint().orElse(null)
        ));
        outbox.commit(new ValidatedEvent(
                permissionId,
                permissionRequest.start(),
                permissionRequest.end(),
                permissionRequest.granularity(),
                permissionRequest.cmRequestId(),
                permissionRequest.conversationId(),
                ValidatedEvent.NeedsToBeSent.NO
        ));
        outbox.commit(new EdaAnswerEvent(
                permissionId,
                PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                "Permission administrator has received the request."
        ));
        acceptPermissionRequest(consentData, permissionRequest);
    }
}
