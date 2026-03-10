// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.springframework.stereotype.Component;

@Component
public class ECONCancelHandler {
    private final AtPermissionRequestRepository repository;
    private final Outbox outbox;

    public ECONCancelHandler(AtPermissionRequestRepository repository, Outbox outbox) {
        this.repository = repository;
        this.outbox = outbox;
    }

    public void handleECONCancel(CMRequestStatus cmRequestStatus) {
        var permissionRequests = repository
                .findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(), cmRequestStatus.cmRequestId())
                .stream()
                .map(EdaPermissionRequest::fromProjection)
                .toList();
        for (var permissionRequest : permissionRequests) {
            var permissionId = permissionRequest.permissionId();
            outbox.commit(
                    new EdaAnswerEvent(permissionId,
                                       PermissionProcessStatus.REVOKED,
                                       cmRequestStatus.message())
            );
        }
    }
}
