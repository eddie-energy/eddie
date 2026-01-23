// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.events.SimpleEvent;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CdsRegionConnectorTest {
    @Mock
    @SuppressWarnings("unused")
    private CdsRegionConnectorMetadata metadata;
    @Mock
    private CdsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private CdsRegionConnector connector;
    @Captor
    private ArgumentCaptor<SimpleEvent> eventCaptor;


    @Test
    void testTerminatePermission_forUnknownPermissionRequest_doesNothing() {
        // Given
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.empty());

        // When
        connector.terminatePermission("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminatePermission_forNotAcceptedPermissionRequest_doesNothing() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.CREATED)
                .build();
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        connector.terminatePermission("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminatePermission_forAcceptedPermissionRequest_emitsTermination() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.ACCEPTED)
                .build();
        when(repository.findByPermissionId("pid"))
                .thenReturn(Optional.of(pr));

        // When
        connector.terminatePermission("pid");

        // Then
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var first = eventCaptor.getAllValues().getFirst();
        assertEquals(PermissionProcessStatus.TERMINATED, first.status());
        var second = eventCaptor.getValue();
        assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, second.status());
    }
}