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
    private CCMORejectHandler ccmoRejectHandler;
    @Mock
    private CCMOAcceptHandler ccmoAcceptedHandler;
    @Mock
    private CCMOAnswerHandler ccmoAnswerHandler;
    @Mock
    private PontonErrorHandler pontonErrorHandler;
    @SuppressWarnings("unused")
    private EdaEventsHandler edaEventsHandler;

    public static Stream<NotificationMessageType> testCmRequestStatusMessage_emitsCorrectEvent() {
        return Stream.of(
                NotificationMessageType.CCMO_ACCEPT,
                NotificationMessageType.CCMO_REJECT,
                NotificationMessageType.CCMO_ANSWER,
                NotificationMessageType.PONTON_ERROR,
                NotificationMessageType.CCMS_ANSWER,
                NotificationMessageType.CCMS_REJECT
        );
    }

    @BeforeEach
    void setUp() {
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        edaEventsHandler = new EdaEventsHandler(
                edaAdapter,
                ccmoAcceptedHandler,
                ccmoRejectHandler,
                ccmoAnswerHandler,
                ccmsHandler,
                pontonErrorHandler
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
            case CCMO_ACCEPT -> verify(ccmoAcceptedHandler).handleCCMOAccept(cmRequestStatus);
            case CCMO_REJECT -> verify(ccmoRejectHandler).handleCCMOReject(cmRequestStatus);
            case CCMO_ANSWER -> verify(ccmoAnswerHandler).handleCCMOAnswer(cmRequestStatus);
            case PONTON_ERROR -> verify(pontonErrorHandler).handlePontonError(cmRequestStatus);
            case CCMS_ANSWER -> verify(ccmsHandler).handleCCMSAnswer(cmRequestStatus);
            case CCMS_REJECT -> verify(ccmsHandler).handleCCMSReject(cmRequestStatus);
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
