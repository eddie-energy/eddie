package energy.eddie.regionconnector.cds.tasks;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.cds.persistence.CdsPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetryTerminationTaskTest {
    @Mock
    private CdsPermissionRequestRepository repository;
    @Mock
    private Outbox outbox;
    @InjectMocks
    private RetryTerminationTask task;

    @Test
    void testRetry_externallyTerminatesPermissionRequests() {
        // Given
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .setStatus(PermissionProcessStatus.FAILED_TO_TERMINATE)
                .build();
        when(repository.findByStatus(PermissionProcessStatus.FAILED_TO_TERMINATE))
                .thenReturn(List.of(pr));

        // When
        task.retry();

        // Then
        verify(outbox).commit(assertArg(e -> assertAll(
                () -> assertEquals("pid", e.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION, e.status())
        )));
    }
}