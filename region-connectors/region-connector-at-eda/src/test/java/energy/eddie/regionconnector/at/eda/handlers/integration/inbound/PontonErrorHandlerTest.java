package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestProjection;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PontonErrorHandlerTest {
    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;

    @Mock
    private AtPermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private PontonErrorHandler handler;

    @Test
    void handleCCMOAnswer_whenValidatedRequest_emitsSentToPa() {
        // Given
        CMRequestStatus cmRequestStatus = new CMRequestStatus(
                NotificationMessageType.CCMO_ANSWER,
                "conversationId",
                null,
                List.of()
        );
        var permissionRequests = List.of(
                projection(PermissionProcessStatus.VALIDATED),
                projection(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
        );
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(permissionRequests);

        // When
        handler.handlePontonError(cmRequestStatus);

        // Then
        verify(outbox, times(3)).commit(edaAnswerEventCaptor.capture());
        assertAll(
                () -> assertEquals(3, edaAnswerEventCaptor.getAllValues().size()),
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   edaAnswerEventCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(PermissionProcessStatus.INVALID,
                                   edaAnswerEventCaptor.getAllValues().get(1).status()),
                () -> assertEquals(PermissionProcessStatus.INVALID,
                                   edaAnswerEventCaptor.getAllValues().getLast().status())
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
