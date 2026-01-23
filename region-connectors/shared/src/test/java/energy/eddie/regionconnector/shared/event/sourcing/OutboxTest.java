// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxTest {
    @Mock
    private EventBus eventBus;
    @Mock
    private PermissionEventRepository eventRepository;

    @Test
    void testCommitEvent_cannotBeWrittenToDataBase_isNotEmitted() {
        // Given
        PermissionEvent event = new TestEvent("pid", PermissionProcessStatus.CREATED);
        when(eventRepository.saveAndFlush(event)).thenReturn(null);
        Outbox outbox = new Outbox(eventBus, eventRepository);

        // When
        outbox.commit(event);

        // Then
        verify(eventBus, never()).emit(event);
    }

    @Test
    void testCommitEvent_isWrittenToDatabase_isEmitted() {
        // Given
        PermissionEvent event = new TestEvent("pid", PermissionProcessStatus.CREATED);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        Outbox outbox = new Outbox(eventBus, eventRepository);

        // When
        outbox.commit(event);

        // Then
        verify(eventBus).emit(event);
    }
}