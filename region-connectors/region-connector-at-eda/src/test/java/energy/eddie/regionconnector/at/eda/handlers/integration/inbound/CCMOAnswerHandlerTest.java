package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.dto.SimpleResponseData;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.models.ConsentData;
import energy.eddie.regionconnector.at.eda.models.ResponseCode;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import energy.eddie.regionconnector.at.eda.requests.restricted.enums.AllowedGranularity;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CCMOAnswerHandlerTest {

    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;

    @Mock
    private AtPermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private CCMOAnswerHandler handler;

    @Test
    void handleCCMOAnswer_whenValidatedRequest_emitsSentToPa() {
        // Given
        CMRequestStatus cmRequestStatus = cmRequestStatus(
                List.of(ResponseCode.CmReqOnl.RECEIVED));

        var permissionRequests = List.of(
                projection(PermissionProcessStatus.VALIDATED),
                projection(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
        );
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(permissionRequests);

        // When
        handler.handleCCMOAnswer(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                     edaAnswerEventCaptor.getValue().status());
    }

    private static CMRequestStatus cmRequestStatus(
            List<Integer> statusCodes
    ) {
        return new CMRequestStatus(
                NotificationMessageType.CCMO_ANSWER,
                "conversationId",
                null,
                List.of(ConsentData.fromResponseData(
                        new SimpleResponseData("", "", statusCodes)
                ))
        );
    }

    private static AtPermissionRequestProjection projection(PermissionProcessStatus s) {
        return new AtPermissionRequestProjectionTest(
                "pid", "connectionId", "cmRequestId", "conversationId",
                LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault()), "dnid", "dsoId", "meteringPointId",
                "consentId", "message",
                AllowedGranularity.PT15M.name(), s.name(), Instant.now()
        );
    }
}
