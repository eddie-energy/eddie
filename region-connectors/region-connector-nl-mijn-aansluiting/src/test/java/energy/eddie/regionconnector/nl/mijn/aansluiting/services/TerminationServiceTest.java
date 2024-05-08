package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TerminationServiceTest {
    @Mock
    private NlPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private TerminationService terminationService;
    @Captor
    private ArgumentCaptor<NlSimpleEvent> captor;

    @Test
    void testTerminate_doesNotEmit_onUnknownPermissionRequest() {
        // Given
        when(repository.existsByPermissionIdAndStatus(any(), any()))
                .thenReturn(false);

        // When
        terminationService.terminate("pid");

        // Then
        verify(outbox, never()).commit(any());
    }

    @Test
    void testTerminate_emits_onPermissionRequest() {
        // Given
        when(repository.existsByPermissionIdAndStatus(any(), any()))
                .thenReturn(true);

        // When
        terminationService.terminate("pid");

        // Then
        verify(outbox).commit(captor.capture());
        assertEquals(PermissionProcessStatus.TERMINATED, captor.getValue().status());
    }
}