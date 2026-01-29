// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.es.datadis.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSentToPermissionAdministratorEvent;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class SentHandlerTest {
    @Mock
    private Outbox outbox;
    @Spy
    private EventBus eventBus = new EventBusImpl();
    @SuppressWarnings("unused")
    @InjectMocks
    private SentHandler sentHandler;

    @Test
    void testAccept_onAcceptance_emitsNothing() {
        // Given
        var event = new EsSentToPermissionAdministratorEvent("pid", "ok");

        // When
        eventBus.emit(event);

        // Then
        verify(outbox, never()).commit(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"nonif", "nopermisos", "unknown"})
    void testAccept_onFailure_emitsInvalid(String response) {
        // Given
        var event = new EsSentToPermissionAdministratorEvent("pid", response);

        // When
        eventBus.emit(event);

        // Then
        verify(outbox).commit(assertArg(other -> assertEquals(PermissionProcessStatus.INVALID, other.status())));
    }
}