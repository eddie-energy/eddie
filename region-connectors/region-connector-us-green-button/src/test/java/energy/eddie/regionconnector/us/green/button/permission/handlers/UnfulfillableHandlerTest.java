// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.permission.events.UsUnfulfillableEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UnfulfillableHandlerTest {
    @Spy
    private final EventBus eventBus = new EventBusImpl();
    @Mock
    private Outbox outbox;
    @InjectMocks
    @SuppressWarnings("unused")
    private UnfulfillableHandler unfulfillableHandler;

    @Test
    void testAccept_unfulfillableEvent_thatRequireExternalTermination_emitsEvent() {
        // Given
        var event = new UsUnfulfillableEvent("pid", true);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox).commit(
                assertArg(res -> assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, res.status()))
        );
    }

    @Test
    void testAccept_unfulfillableEvent_thatDoesNotRequireExternalTermination_emitsNothing() {
        // Given
        var event = new UsUnfulfillableEvent("pid", false);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox, never()).commit(any());
    }
}