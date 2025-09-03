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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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

    private static AtPermissionRequestProjection projection(PermissionProcessStatus permissionProcessStatus) {
        AtPermissionRequestProjection p = mock(AtPermissionRequestProjection.class);

        when(p.getPermissionId()).thenReturn("pid");
        when(p.getConnectionId()).thenReturn("connectionId");
        when(p.getCmRequestId()).thenReturn("cmRequestId");
        when(p.getConversationId()).thenReturn("conversationId");
        when(p.getPermissionStart()).thenReturn(LocalDate.now());
        when(p.getPermissionEnd()).thenReturn(LocalDate.now());
        when(p.getDataNeedId()).thenReturn("dnid");
        when(p.getDsoId()).thenReturn("dsoId");
        when(p.getMeteringPointId()).thenReturn("meteringPointId");
        when(p.getConsentId()).thenReturn("consentId");
        when(p.getMessage()).thenReturn("message");
        when(p.getGranularity()).thenReturn(AllowedGranularity.PT15M.name());
        when(p.getStatus()).thenReturn(permissionProcessStatus.name());
        when(p.getCreated()).thenReturn(Instant.now());

        return p;
    }
}
