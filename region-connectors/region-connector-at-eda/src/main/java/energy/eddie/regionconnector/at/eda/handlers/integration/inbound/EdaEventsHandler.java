// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class EdaEventsHandler {
    private final CCMOAcceptHandler ccmoAcceptedHandler;
    private final CCMORejectHandler ccmoRejectHandler;
    private final CCMOAnswerHandler ccmoAnswerHandler;
    private final CCMSHandler ccmsHandler;
    private final PontonErrorHandler pontonErrorHandler;

    public EdaEventsHandler(
            EdaAdapter edaAdapter,
            CCMOAcceptHandler ccmoAcceptedHandler,
            CCMORejectHandler ccmoRejectHandler,
            CCMOAnswerHandler ccmoAnswerHandler,
            CCMSHandler ccmsHandler,
            PontonErrorHandler pontonErrorHandler
    ) {
        this.ccmoAcceptedHandler = ccmoAcceptedHandler;
        this.ccmoRejectHandler = ccmoRejectHandler;
        this.ccmoAnswerHandler = ccmoAnswerHandler;
        this.ccmsHandler = ccmsHandler;
        this.pontonErrorHandler = pontonErrorHandler;
        edaAdapter.getCMRequestStatusStream()
                  .subscribe(this::transitionPermissionRequest);
    }

    private void transitionPermissionRequest(CMRequestStatus cmRequestStatus) {
        switch (cmRequestStatus.messageType()) {
            case CCMO_ACCEPT -> ccmoAcceptedHandler.handleCCMOAccept(cmRequestStatus);
            case CCMO_REJECT -> ccmoRejectHandler.handleCCMOReject(cmRequestStatus);
            case CCMO_ANSWER -> ccmoAnswerHandler.handleCCMOAnswer(cmRequestStatus);
            case PONTON_ERROR -> pontonErrorHandler.handlePontonError(cmRequestStatus);
            case CCMS_ANSWER -> ccmsHandler.handleCCMSAnswer(cmRequestStatus);
            case CCMS_REJECT -> ccmsHandler.handleCCMSReject(cmRequestStatus);
        }
    }
}
