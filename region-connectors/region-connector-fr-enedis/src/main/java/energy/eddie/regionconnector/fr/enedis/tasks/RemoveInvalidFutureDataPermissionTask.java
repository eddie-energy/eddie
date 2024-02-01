package energy.eddie.regionconnector.fr.enedis.tasks;


import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Sinks;

import java.util.List;
import java.util.NoSuchElementException;

public class RemoveInvalidFutureDataPermissionTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveInvalidFutureDataPermissionTask.class);
    private final List<FutureDataPermission> futureDataPermissions;
    private final FutureDataPermissionRepository futureDataPermissionRepository;
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;

    public RemoveInvalidFutureDataPermissionTask(List<FutureDataPermission> futureDataPermissions, FutureDataPermissionRepository futureDataPermissionRepository, PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository, Sinks.Many<ConnectionStatusMessage> connectionStatusSink) {
        this.futureDataPermissions = futureDataPermissions;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
    }

    private void changeFutureDataPermissionStates() {
        for (var futureDataPermission : futureDataPermissions) {
            try {
                var permissionRequest = permissionRequestRepository.findByPermissionId(futureDataPermission.permissionId()).orElseThrow();
                permissionRequest.fulfill();
                connectionStatusSink.tryEmitNext(
                        new ConnectionStatusMessage(
                                permissionRequest.connectionId(),
                                permissionRequest.permissionId(),
                                permissionRequest.dataNeedId(),
                                permissionRequest.dataSourceInformation(),
                                permissionRequest.state().status()
                        )
                );
            } catch (StateTransitionException e) {
                LOGGER.error("PermissionRequest with permissionID {} cannot be set to fulfilled", futureDataPermission.permissionId(), e);
            } catch (NoSuchElementException e) {
                LOGGER.error("PermissionRequest with permissionID {} could not be found.", futureDataPermission.permissionId(), e);
            }
        }
    }

    @Override
    public void run() {
        changeFutureDataPermissionStates();
        futureDataPermissionRepository.deleteAllInBatch(futureDataPermissions);
    }
}
