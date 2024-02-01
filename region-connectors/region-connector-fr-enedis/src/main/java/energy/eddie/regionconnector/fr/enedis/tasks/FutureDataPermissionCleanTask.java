package energy.eddie.regionconnector.fr.enedis.tasks;

import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.NoSuchElementException;

public class FutureDataPermissionCleanTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataPermissionCleanTask.class);
    private final List<FutureDataPermission> futureDataPermissions;
    private final FutureDataPermissionRepository futureDataPermissionRepository;
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;

    public FutureDataPermissionCleanTask(List<FutureDataPermission> futureDataPermissions, FutureDataPermissionRepository futureDataPermissionRepository, PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository) {
        this.futureDataPermissions = futureDataPermissions;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    private void changeFutureDataPermissionStates() {
        for (var futureDataPermission : futureDataPermissions) {
            try {
                permissionRequestRepository.findByPermissionId(futureDataPermission.permissionId()).orElseThrow().timeLimit();
            } catch (StateTransitionException e) {
                LOGGER.error("PermissionRequest with permissionID {} cannot be set to time limit", futureDataPermission.permissionId(), e);
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
