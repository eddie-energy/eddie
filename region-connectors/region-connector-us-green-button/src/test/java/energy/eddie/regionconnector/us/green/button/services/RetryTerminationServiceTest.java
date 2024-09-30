package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static energy.eddie.regionconnector.us.green.button.PermissionRequestCreator.createPermissionRequest;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryTerminationServiceTest {
    @Mock
    private UsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private RetryTerminationService retryTerminationService;

    @Test
    void testRetryTermination_emitsRequiresExternalTerminationEvent() {
        // Given
        var pr = createPermissionRequest();
        when(repository.findAllByStatus(PermissionProcessStatus.FAILED_TO_TERMINATE))
                .thenReturn(List.of(pr));

        // When
        retryTerminationService.retryTermination();

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, event.status())
        )));
    }
}