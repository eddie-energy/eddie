// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.us.green.button;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GreenButtonRegionConnectorTest {
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private GreenButtonRegionConnector regionConnector;
    @Captor
    private ArgumentCaptor<PermissionEvent> eventCaptor;


    @Test
    void testGetMetadata_returnsMetadata() {
        // Given
        // When
        var res = regionConnector.getMetadata();

        // Then
        assertEquals(GreenButtonRegionConnectorMetadata.getInstance(), res);
    }

    @Test
    void testTerminate_emitsTerminationEvents_forExistingPermissionRequest() {
        // Given
        when(repository.existsByPermissionIdAndStatus("pid", PermissionProcessStatus.ACCEPTED))
                .thenReturn(true);

        // When
        regionConnector.terminatePermission("pid");

        // Then
        verify(outbox, times(2)).commit(eventCaptor.capture());
        var terminatedEvent = eventCaptor.getAllValues().getFirst();
        assertAll(
                () -> assertEquals("pid", terminatedEvent.permissionId()),
                () -> assertEquals(PermissionProcessStatus.TERMINATED, terminatedEvent.status())
        );
        var externalTerminationEvent = eventCaptor.getValue();
        assertAll(
                () -> assertEquals("pid", externalTerminationEvent.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION,
                                   externalTerminationEvent.status())
        );
    }

    @Test
    void testTerminate_emitsNoEvent_forNonExistentPermissionRequest() {
        // Given
        when(repository.existsByPermissionIdAndStatus("pid", PermissionProcessStatus.ACCEPTED))
                .thenReturn(false);

        // When
        regionConnector.terminatePermission("pid");

        // Then
        verify(outbox, never()).commit(any());
    }
}