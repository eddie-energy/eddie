package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessagingExtensionTest {
    @Test
    void extension_sendsMessage() {
        // Given
        Sinks.Many<ConnectionStatusMessage> sink = Sinks.many().multicast().onBackpressureBuffer();
        MessagingExtension<PermissionRequest> messagingExtension = new MessagingExtension<>(sink);
        PermissionRequestState state = mock(PermissionRequestState.class);
        when(state.status()).thenReturn(PermissionProcessStatus.CREATED);
        PermissionRequest permissionRequest = new SimplePermissionRequest(
                "pid",
                "cid",
                state,
                "dnid"
        );

        // When
        messagingExtension.accept(permissionRequest);

        // Then
        StepVerifier.create(sink.asFlux())
                .then(sink::tryEmitComplete)
                .assertNext(csm ->
                        assertAll(
                                () -> assertEquals(PermissionProcessStatus.CREATED, csm.status()),
                                () -> assertEquals(permissionRequest.permissionId(), csm.permissionId())
                        )
                )
                .expectComplete()
                .verify();
    }
}