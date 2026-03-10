// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import org.springframework.stereotype.Component;

@Component
public class EdaEventsHandler {
    private final CMAcceptHandler cmAcceptedHandler;
    private final CMRejectHandler cmRejectHandler;
    private final CMAnswerHandler cmAnswerHandler;
    private final CCMSHandler ccmsHandler;
    private final PontonErrorHandler pontonErrorHandler;
    private final ECONCancelHandler econCancelHandler;

    public EdaEventsHandler(
            EdaAdapter edaAdapter,
            CMAcceptHandler cmAcceptedHandler,
            CMRejectHandler cmRejectHandler,
            CMAnswerHandler cmAnswerHandler,
            CCMSHandler ccmsHandler,
            PontonErrorHandler pontonErrorHandler,
            ECONCancelHandler econCancelHandler
    ) {
        this.cmAcceptedHandler = cmAcceptedHandler;
        this.cmRejectHandler = cmRejectHandler;
        this.cmAnswerHandler = cmAnswerHandler;
        this.ccmsHandler = ccmsHandler;
        this.pontonErrorHandler = pontonErrorHandler;
        this.econCancelHandler = econCancelHandler;
        edaAdapter.getCMRequestStatusStream()
                  .subscribe(this::transitionPermissionRequest);
    }

    private void transitionPermissionRequest(CMRequestStatus cmRequestStatus) {
        switch (cmRequestStatus.messageType()) {
            case CCMO_ACCEPT, ECON_ACCEPT -> cmAcceptedHandler.handleCMAccept(cmRequestStatus);
            case CCMO_REJECT, ECON_REJECT -> cmRejectHandler.handleCMReject(cmRequestStatus);
            case CCMO_ANSWER, ECON_ANSWER -> cmAnswerHandler.handleCMAnswer(cmRequestStatus);
            case CCMS_ANSWER -> ccmsHandler.handleCCMSAnswer(cmRequestStatus);
            case CCMS_REJECT -> ccmsHandler.handleCCMSReject(cmRequestStatus);
            case ECON_CANCEL -> econCancelHandler.handleECONCancel(cmRequestStatus);
            case PONTON_ERROR -> pontonErrorHandler.handlePontonError(cmRequestStatus);
        }
    }
}
