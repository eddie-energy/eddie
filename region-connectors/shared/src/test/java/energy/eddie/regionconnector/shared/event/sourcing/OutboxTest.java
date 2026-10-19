// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxTest {
    @Mock
    private EventBus eventBus;
    @Mock
    private PermissionEventRepository eventRepository;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void commit_inTransaction_emitsInOrderOnlyAfterCommit() {
        var first = new TestEvent("pid", PermissionProcessStatus.CREATED);
        var second = new TestEvent("pid", PermissionProcessStatus.ACCEPTED);
        when(eventRepository.saveAndFlush(first)).thenReturn(first);
        when(eventRepository.saveAndFlush(second)).thenReturn(second);
        TransactionSynchronizationManager.initSynchronization();
        var outbox = new Outbox(eventBus, eventRepository);

        outbox.commit(first);
        outbox.commit(second);

        verifyNoInteractions(eventBus);
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(2, synchronizations.size());
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        var order = inOrder(eventBus);
        order.verify(eventBus).emit(first);
        order.verify(eventBus).emit(second);
    }

    @Test
    void commit_rolledBackTransaction_doesNotEmitEvents() {
        var event = new TestEvent("pid", PermissionProcessStatus.ACCEPTED);
        when(eventRepository.saveAndFlush(event)).thenReturn(event);
        TransactionSynchronizationManager.initSynchronization();

        new Outbox(eventBus, eventRepository).commit(event);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(eventBus);
    }

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
