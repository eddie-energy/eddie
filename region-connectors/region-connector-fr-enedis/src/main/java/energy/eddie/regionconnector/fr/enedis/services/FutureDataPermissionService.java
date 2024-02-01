package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import energy.eddie.regionconnector.fr.enedis.tasks.FutureDataPermissionCleanTask;
import energy.eddie.regionconnector.fr.enedis.tasks.FutureDataPermissionPollTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class FutureDataPermissionService {
    private static final String ACCEPTED = PermissionProcessStatus.ACCEPTED.toString();
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");
    private static final int TASKS_PER_THREAD = 50;
    private final FutureDataPermissionRepository futureDataPermissionRepository;
    private final AsyncTaskExecutor taskExecutor;
    private final PollingService pollingService;
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;

    public FutureDataPermissionService(PollingService pollingService, @Qualifier("taskExecutor") AsyncTaskExecutor taskExecutor, FutureDataPermissionRepository futureDataPermissionRepository, PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository) {
        this.pollingService = pollingService;
        this.taskExecutor = taskExecutor;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public void terminateFutureDataPermission(TimeframedPermissionRequest permissionRequest) {
        var futureDataPermission = futureDataPermissionRepository.findFutureDataPermissionByPermissionId(permissionRequest.permissionId());
        futureDataPermission.withState(permissionRequest.state());
        futureDataPermissionRepository.saveAndFlush(futureDataPermission);
    }

    @Scheduled(cron = "${region-connector.fr.enedis.tasks.cron.future.data.permission.poll}")
    private void pollFutureData() {
        List<FutureDataPermission> futureDataPermissions = getValidFutureDataPermissions(ZonedDateTime.now(ZONE_ID).toInstant());
        for (int i = 0; i < futureDataPermissions.size(); i += TASKS_PER_THREAD) {
            int end = Math.min(i + TASKS_PER_THREAD, futureDataPermissions.size());
            List<FutureDataPermission> batch = futureDataPermissions.subList(i, end);

            taskExecutor.execute(new FutureDataPermissionPollTask(pollingService, batch, futureDataPermissionRepository));
        }
    }

    @Scheduled(cron = "${region-connector.fr.enedis.tasks.cron.future.data.permission.clean}")
    private void cleanUpFutureDataPermission() {
        List<FutureDataPermission> futureDataPermissions = getInvalidFutureDataPermissions(ZonedDateTime.now(ZONE_ID).toInstant());

        for (int i = 0; i < futureDataPermissions.size(); i += TASKS_PER_THREAD) {
            int end = Math.min(i + TASKS_PER_THREAD, futureDataPermissions.size());
            List<FutureDataPermission> batch = futureDataPermissions.subList(i, end);

            taskExecutor.execute(new FutureDataPermissionCleanTask(batch, futureDataPermissionRepository, permissionRequestRepository));
        }
    }

    private List<FutureDataPermission> getValidFutureDataPermissions(Instant today) {

        return futureDataPermissionRepository.findAllByValidToAfterAndStateEquals(today, ACCEPTED);
    }

    private List<FutureDataPermission> getInvalidFutureDataPermissions(Instant today) {
        return futureDataPermissionRepository.findAllByValidToBeforeOrStateIsNot(today, ACCEPTED);
    }
}
