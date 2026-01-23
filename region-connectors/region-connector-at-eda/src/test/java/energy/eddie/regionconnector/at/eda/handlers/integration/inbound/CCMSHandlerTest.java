// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CCMSHandlerTest {

    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;

    @Mock
    private AtPermissionRequestRepository repository;

    @Mock
    private Outbox outbox;

    @InjectMocks
    private CCMSHandler handler;

    public static Stream<Arguments> ccmsRejectOptions() {
        return Stream.of(
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.TERMINATION_SUCCESSFUL)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.CONSENT_AND_METERINGPOINT_DO_NOT_MATCH)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.INVALID_PROCESSDATE)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.CONSENT_ID_EXPIRED)),
                Arguments.of(NotificationMessageType.CCMS_REJECT,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED,
                             List.of(ResponseCode.CmRevSP.NO_CONSENT_PRESENT))
        );
    }

    @ParameterizedTest
    @MethodSource("ccmsRejectOptions")
    void handleCCMSReject_alwaysEmitsExternallyTerminatedForRequiresExternalTermination(
            NotificationMessageType notificationMessageType,
            PermissionProcessStatus status,
            List<Integer> statusCodes
    ) {
        // Given
        var permissionRequests = List.of(
                projection(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION),
                projection(PermissionProcessStatus.ACCEPTED));

        CMRequestStatus cmRequestStatus = cmRequestStatus(notificationMessageType, statusCodes);
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(permissionRequests);

        // When
        handler.handleCCMSReject(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(status, edaAnswerEventCaptor.getValue().status());
    }

    private static AtPermissionRequestProjection projection(PermissionProcessStatus s) {
        return new AtPermissionRequestProjectionTest(
                "pid", "connectionId", "cmRequestId", "conversationId",
                LocalDate.now(ZoneId.systemDefault()), LocalDate.now(ZoneId.systemDefault()), "dnid", "dsoId", "meteringPointId", "consentId", "message",
                AllowedGranularity.PT15M.name(), s.name(), Instant.now()
        );
    }

    private static CMRequestStatus cmRequestStatus(
            NotificationMessageType ccmsAnswer,
            List<Integer> statusCodes
    ) {
        return new CMRequestStatus(
                ccmsAnswer,
                "conversationId",
                null,
                List.of(ConsentData.fromResponseData(
                        new SimpleResponseData("", "", statusCodes)
                ))
        );
    }

    @Test
    void handleCCMSAnswer_emitsExternallyTerminatedForEveryRequiresTerminationPermissionRequest() {
        // Given
        var permissionRequest = projection(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION);
        CMRequestStatus cmRequestStatus = cmRequestStatus(NotificationMessageType.CCMS_ANSWER,
                                                          List.of(ResponseCode.CmRevSP.TERMINATION_SUCCESSFUL));

        var accepted = projection(PermissionProcessStatus.ACCEPTED);
        when(repository.findByConversationIdOrCMRequestId(cmRequestStatus.conversationId(),
                                                          cmRequestStatus.cmRequestId()))
                .thenReturn(List.of(permissionRequest,
                                    permissionRequest,
                                    accepted));

        // When
        handler.handleCCMSAnswer(cmRequestStatus);

        // Then
        verify(outbox, times(2)).commit(edaAnswerEventCaptor.capture());
        assertAll(
                () -> assertEquals(2, edaAnswerEventCaptor.getAllValues().size()),
                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                   edaAnswerEventCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                   edaAnswerEventCaptor.getAllValues().getLast().status())
        );
    }


}
