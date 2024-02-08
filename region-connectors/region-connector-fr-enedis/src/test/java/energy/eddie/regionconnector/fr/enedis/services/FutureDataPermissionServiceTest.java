package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.AsyncTaskExecutor;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FutureDataPermissionServiceTest {
    @Mock
    AsyncTaskExecutor asyncTaskExecutor;
    @Mock
    private PollingService pollingService;
    @Mock
    private FutureDataPermissionRepository futureDataPermissionRepository;

    @Mock
    private Sinks.Many<ConnectionStatusMessage> connectionStatusSink;

    @Mock
    private PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;

    @Test
    void terminateFutureDataPermissionTest() {
        // Given
        var permissionId = "permissionId";
        var permissionRequest = mock(TimeframedPermissionRequest.class);
        var permissionRequestState = mock(PermissionRequestState.class);
        when(permissionRequest.permissionId()).thenReturn(permissionId);
        when(permissionRequest.state()).thenReturn(permissionRequestState);
        when(permissionRequestState.status()).thenReturn(PermissionProcessStatus.TERMINATED);

        var futureDataPermission = new FutureDataPermission();
        futureDataPermission.withPermissionId(permissionId);

        when(futureDataPermissionRepository.findFutureDataPermissionByPermissionId(permissionId)).thenReturn(futureDataPermission);

        FutureDataPermissionService futureDataPermissionService = new FutureDataPermissionService(
                pollingService,
                asyncTaskExecutor,
                futureDataPermissionRepository,
                permissionRequestRepository,
                connectionStatusSink
        );

        // When
        futureDataPermissionService.terminateFutureDataPermission(permissionRequest);

        // Then
        verify(permissionRequest, times(1)).state();
        verify(futureDataPermissionRepository, times(1)).findFutureDataPermissionByPermissionId(permissionId);
        verify(futureDataPermissionRepository, times(1)).saveAndFlush(futureDataPermission);
        assertEquals(permissionRequest.state(), futureDataPermission.state());
    }
}
