// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EdaEventsHandlerTest {
    TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private CCMSHandler ccmsHandler;
    @Mock
    private CMRejectHandler cmRejectHandler;
    @Mock
    private CMAcceptHandler ccmoAcceptedHandler;
    @Mock
    private CMAnswerHandler cmAnswerHandler;
    @Mock
    private PontonErrorHandler pontonErrorHandler;
    @Mock
    private ECONCancelHandler econCancelHandler;

    public static Stream<NotificationMessageType> testCmRequestStatusMessage_emitsCorrectEvent() {
        return Stream.of(
                NotificationMessageType.CCMO_ACCEPT,
                NotificationMessageType.ECON_ACCEPT,
                NotificationMessageType.CCMO_REJECT,
                NotificationMessageType.ECON_REJECT,
                NotificationMessageType.CCMO_ANSWER,
                NotificationMessageType.ECON_ANSWER,
                NotificationMessageType.PONTON_ERROR,
                NotificationMessageType.CCMS_ANSWER,
                NotificationMessageType.CCMS_REJECT,
                NotificationMessageType.ECON_CANCEL
        );
    }

    @BeforeEach
    void setUp() {
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        new EdaEventsHandler(
                edaAdapter,
                ccmoAcceptedHandler,
                cmRejectHandler,
                cmAnswerHandler,
                ccmsHandler,
                pontonErrorHandler,
                econCancelHandler
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCmRequestStatusMessage_emitsCorrectEvent(
            NotificationMessageType notificationMessageType
    ) {
        // Given
        CMRequestStatus cmRequestStatus = cmRequestStatus(notificationMessageType);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        switch (notificationMessageType) {
            case CCMO_ACCEPT, ECON_ACCEPT -> verify(ccmoAcceptedHandler).handleCMAccept(cmRequestStatus);
            case CCMO_REJECT, ECON_REJECT -> verify(cmRejectHandler).handleCMReject(cmRequestStatus);
            case CCMO_ANSWER, ECON_ANSWER -> verify(cmAnswerHandler).handleCMAnswer(cmRequestStatus);
            case PONTON_ERROR -> verify(pontonErrorHandler).handlePontonError(cmRequestStatus);
            case CCMS_ANSWER -> verify(ccmsHandler).handleCCMSAnswer(cmRequestStatus);
            case CCMS_REJECT -> verify(ccmsHandler).handleCCMSReject(cmRequestStatus);
            case ECON_CANCEL -> verify(econCancelHandler).handleECONCancel(cmRequestStatus);
        }
    }

    private static CMRequestStatus cmRequestStatus(
            NotificationMessageType notificationMessageType
    ) {
        return new CMRequestStatus(
                notificationMessageType,
                "conversationId",
                null,
                List.of()
        );
    }
}
