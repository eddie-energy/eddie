package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PermissionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MessagingPermissionRequestTest {
    static Stream<Arguments> messagingPermissionRequestArguments() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, "sendToPermissionAdministrator"),
                Arguments.of(PermissionProcessStatus.UNABLE_TO_SEND, "sendToPermissionAdministrator"),
                Arguments.of(PermissionProcessStatus.RECEIVED_PERMISSION_ADMINISTRATOR_RESPONSE, "receivedPermissionAdministratorResponse"),
                Arguments.of(PermissionProcessStatus.TERMINATED, "terminate"),
                Arguments.of(PermissionProcessStatus.ACCEPTED, "accept"),
                Arguments.of(PermissionProcessStatus.INVALID, "invalid"),
                Arguments.of(PermissionProcessStatus.REJECTED, "reject"),
                Arguments.of(PermissionProcessStatus.VALIDATED, "validate"),
                Arguments.of(PermissionProcessStatus.MALFORMED, "validate")
        );
    }

    @ParameterizedTest
    @MethodSource("messagingPermissionRequestArguments")
    void messagingPermissionRequest_emitsUnderlyingStatus_whenActionInvoked(
            PermissionProcessStatus expectedStatus,
            String actionMethodName
    ) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        TransitionableState createdState = new TransitionableState(PermissionProcessStatus.CREATED, expectedStatus);
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        Method actionMethod = MessagingPermissionRequest.class.getMethod(actionMethodName);
        actionMethod.invoke(messagingPermissionRequest);
        permissionStateMessages.tryEmitComplete();

        // Then
        StepVerifier.create(permissionStateMessages.asFlux())
                .consumeNextWith(cr -> {
                })
                .assertNext(cr -> assertAll(
                        () -> assertEquals(expectedStatus, cr.status()),
                        () -> assertEquals("permissionId", cr.permissionId()),
                        () -> assertEquals("connectionId", cr.connectionId())
                ))
                .verifyComplete();
    }

    @Test
    void messagingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
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
        SimpleState initialState = new SimpleState();
        TransitionableState changedState = new TransitionableState(PermissionProcessStatus.CREATED, PermissionProcessStatus.ACCEPTED);
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", initialState, "dataNeedId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        messagingPermissionRequest.changeState(changedState);

        // Then
        assertEquals(changedState, messagingPermissionRequest.state());
    }

    @Test
    void messagingPermissionRequest_emitsCreatedState_uponCreation() {
        // Given
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState(PermissionProcessStatus.CREATED);
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");

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
    void messagingPermissionRequest_equalsReturnsTrueForDecoratee() {
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        var res = messagingPermissionRequest.equals(permissionRequest);

        // Then
        assertTrue(res);
    }

    @Test
    void messagingPermissionRequest_equalsReturnsFalse() {
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest1 = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest1, permissionStateMessages);

        // When
        var res = messagingPermissionRequest.equals(new Object());

        // Then
        assertFalse(res);
    }

    @Test
    void messagingPermissionRequest_hashCodeIsSameForDecoratee() {
        Sinks.Many<ConnectionStatusMessage> permissionStateMessages = Sinks.many().unicast().onBackpressureBuffer();
        SimpleState createdState = new SimpleState();
        PermissionRequest permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState, "dataNeedId");
        MessagingPermissionRequest messagingPermissionRequest = new MessagingPermissionRequest(permissionRequest, permissionStateMessages);

        // When
        var res = messagingPermissionRequest.hashCode();

        // Then
        assertEquals(permissionRequest.hashCode(), res);
    }
}