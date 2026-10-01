// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.config.AtConfiguration;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.at.eda.permission.request.events.TerminationEvent;
import energy.eddie.regionconnector.at.eda.requests.EdaGroupingIdFactory;
import energy.eddie.regionconnector.shared.event.sourcing.EventBusImpl;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TerminationHandlerTest {
    @Mock
    private Outbox outbox;

    @Test
    void givenTerminatedEvent_thenCreateNewConversationIdAndPublishRequiresExternalTermination() {
        // Given
        var eventBus = new EventBusImpl();
        var config = new AtConfiguration("test", null, "");
        var factory = new EdaGroupingIdFactory(config);
        new TerminationHandler(outbox, eventBus, factory);

        // When
        eventBus.emit(new SimpleEvent("pid", PermissionProcessStatus.TERMINATED));

        // Then
        verify(outbox).commit(isA(TerminationEvent.class));
    }
}