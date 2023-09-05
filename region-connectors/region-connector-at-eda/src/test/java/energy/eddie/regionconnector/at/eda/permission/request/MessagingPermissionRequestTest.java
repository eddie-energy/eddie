package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.FutureStateException;
import energy.eddie.regionconnector.at.api.PastStateException;
import energy.eddie.regionconnector.at.api.PermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagingPermissionRequestTest {

    @Test
    void messagingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        CreatedPermissionRequestState createdState = new CreatedPermissionRequestState(null, null, null);
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", "rid", "cid", createdState);
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        var state = messagingPermissionRequest.state();

        // Then
        assertEquals(createdState, state);
    }

    @Test
    void messagingPermissionRequest_changesStateOfWrappedPermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        CreatedPermissionRequestState createdState = new CreatedPermissionRequestState(null, null, null);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, messagingPermissionRequest.state());
    }

    @Test
    void messagingPermissionRequest_returnsCMRequestId() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        String cmRequestId = messagingPermissionRequest.cmRequestId();

        // Then
        assertEquals("cmRequestId", cmRequestId);
    }

    @Test
    void messagingPermissionRequest_returnsConversationId() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        String conversationId = messagingPermissionRequest.conversationId();

        // Then
        assertEquals("messageId", conversationId);
    }

    @Test
    void messagingPermissionRequest_emitsCreatedState_uponCreation() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");

        // When
        new MessagingPermissionRequest(permissionRequest, permissionStateMessages);
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.CREATED, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsValidated_whenValidated() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.validate();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.VALIDATED, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsSentToPermissionAdministrator_whenSentToPermissionAdministrator() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.sendToPermissionAdministrator();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsReceivedPermissionAdministratorResponse_whenReceivedPermissionAdministratorResponse() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.receivedPermissionAdministratorResponse();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsTerminated_whenTerminated() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.terminate();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.TERMINATED, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsAccepted_whenAccepted() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.accept();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.ACCEPTED, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsInvalid_whenInvalid() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.invalid();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.INVALID, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_emitsRejected_whenRejected() throws FutureStateException, PastStateException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.rejected();
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(PermissionProcessStatus.REJECTED, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }
}