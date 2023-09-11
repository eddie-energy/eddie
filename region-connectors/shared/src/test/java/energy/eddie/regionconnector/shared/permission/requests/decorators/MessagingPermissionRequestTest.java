package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessagingPermissionRequestTest {
    @Test
    void messagingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

        // When
        var state = messagingPermissionRequest.state();

        // Then
        assertEquals(createdState, state);
    }

    @Test
    void messagingPermissionRequest_changesStateOfWrappedPermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, messagingPermissionRequest.state());
    }

    @Test
    void messagingPermissionRequest_emitsCreatedState_uponCreation() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);

        // When
        new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);
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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        MessagingPermissionRequest<PermissionRequest> messagingPermissionRequest = new MessagingPermissionRequest<>(permissionRequest, permissionStateMessages);

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