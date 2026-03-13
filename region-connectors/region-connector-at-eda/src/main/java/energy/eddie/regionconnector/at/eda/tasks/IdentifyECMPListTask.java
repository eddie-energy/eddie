// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.tasks;

import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.EdaECMPList;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableECMPList;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IdentifyECMPListTask {
    private final AtPermissionRequestRepository repository;

    public IdentifyECMPListTask(AtPermissionRequestRepository repository) {this.repository = repository;}

    public Optional<IdentifiableECMPList> identify(EdaECMPList ecmpList) {
        var prs = repository.findByConversationIdOrCMRequestId(ecmpList.conversationId(), null);
        if (prs.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new IdentifiableECMPList(ecmpList, EdaPermissionRequest.fromProjection(prs.getFirst())));
    }
}
