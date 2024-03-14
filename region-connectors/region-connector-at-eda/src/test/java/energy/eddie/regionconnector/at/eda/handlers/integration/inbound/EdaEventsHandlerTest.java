package energy.eddie.regionconnector.at.eda.handlers.integration.inbound;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.models.CMRequestStatus;
import energy.eddie.regionconnector.at.eda.permission.request.EdaPermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.events.AcceptedEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.EdaAnswerEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.publisher.TestPublisher;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdaEventsHandlerTest {
    @Captor
    ArgumentCaptor<AcceptedEvent> acceptedEventCaptor;
    @Mock
    private AtPermissionRequestRepository repository;
    @Mock
    private EdaAdapter edaAdapter;
    @Mock
    private Outbox outbox;
    @Captor
    private ArgumentCaptor<EdaAnswerEvent> edaAnswerEventCaptor;

    public static Stream<Arguments> testAcceptedMessage_doesNotEmitOnMissingPayload() {
        return Stream.of(
                Arguments.of("mid", null),
                Arguments.of(null, "consentId")
        );
    }

    public static Stream<Arguments> testCmRequestStatusMessage_emitsCorrectEvent() {
        return Stream.of(
                Arguments.of(CMRequestStatus.Status.ERROR, PermissionProcessStatus.INVALID),
                Arguments.of(CMRequestStatus.Status.REJECTED, PermissionProcessStatus.REJECTED),
                Arguments.of(CMRequestStatus.Status.RECEIVED, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
        );
    }

    @Test
    void testAcceptedMessage_emitsAcceptedEvent() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", "cmRequestId"))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "conversationId");
        cmRequestStatus.setMeteringPoint("mid");
        cmRequestStatus.setCmConsentId("consentId");
        cmRequestStatus.setCmRequestId("cmRequestId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(acceptedEventCaptor.capture());
        assertAll(
                () -> assertEquals("mid", acceptedEventCaptor.getValue().meteringPointId()),
                () -> assertEquals("consentId", acceptedEventCaptor.getValue().cmConsentId())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testAcceptedMessage_doesNotEmitOnMissingPayload(String meteringPoint, String consentId) {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", "cmRequestId"))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "conversationId");
        cmRequestStatus.setMeteringPoint(meteringPoint);
        cmRequestStatus.setCmConsentId(consentId);
        cmRequestStatus.setCmRequestId("cmRequestId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testErrorMessage_forNotAcknowledgedPermissionRequest_emitsAcknowledgmentAndInvalidEvent() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.PENDING_PERMISSION_ADMINISTRATOR_ACKNOWLEDGEMENT,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ERROR, "", "conversationId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, times(2)).commit(edaAnswerEventCaptor.capture());
        assertAll(
                () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                   edaAnswerEventCaptor.getAllValues().getFirst().status()),
                () -> assertEquals(PermissionProcessStatus.INVALID, edaAnswerEventCaptor.getAllValues().get(1).status())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCmRequestStatusMessage_emitsCorrectEvent(CMRequestStatus.Status cmStatus, PermissionProcessStatus status) {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(cmStatus, "", "conversationId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox).commit(edaAnswerEventCaptor.capture());
        assertEquals(status, edaAnswerEventCaptor.getValue().status());
    }

    @ParameterizedTest
    @EnumSource(value = CMRequestStatus.Status.class, names = {"SENT", "DELIVERED"})
    void testUnmappedCmRequestStatusMessage_doesNotEmitEvent(CMRequestStatus.Status cmStatus) {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", "dnid", "cmRequestId",
                                                         "conversationId", null, "dsoId", null, null,
                                                         Granularity.PT15M,
                                                         PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                                                         "", null, null);
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.of(permissionRequest));
        CMRequestStatus cmRequestStatus = new CMRequestStatus(cmStatus, "", "conversationId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testUnknownCmRequestStatus_doesNotEmit() {
        // Given
        TestPublisher<CMRequestStatus> publisher = TestPublisher.create();
        when(edaAdapter.getCMRequestStatusStream()).thenReturn(publisher.flux());
        when(repository.findByConversationIdOrCMRequestId("conversationId", null))
                .thenReturn(Optional.empty());
        CMRequestStatus cmRequestStatus = new CMRequestStatus(CMRequestStatus.Status.ACCEPTED, "", "conversationId");
        new EdaEventsHandler(edaAdapter, outbox, repository);

        // When
        publisher.emit(cmRequestStatus);

        // Then
        verify(outbox, never()).commit(any());
    }


}