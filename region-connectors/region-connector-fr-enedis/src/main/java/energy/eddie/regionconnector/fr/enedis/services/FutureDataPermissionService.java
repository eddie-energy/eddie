package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import energy.eddie.regionconnector.fr.enedis.tasks.PollFutureDataTask;
import energy.eddie.regionconnector.fr.enedis.tasks.RemoveInvalidFutureDataPermissionTask;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;

@Service
public class FutureDataPermissionService {
    private static final String ACCEPTED = PermissionProcessStatus.ACCEPTED.toString();
    private final FutureDataPermissionRepository futureDataPermissionRepository;
    private final AsyncTaskExecutor taskExecutor;
    private final PollingService pollingService;
    private final PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository;
    private final Sinks.Many<ConnectionStatusMessage> connectionStatusSink;
    @Value(value = "${region-connector.fr.enedis.tasks.threads.per.task}")
    private int tasksPerThread = 50;

    public FutureDataPermissionService(PollingService pollingService, @Qualifier("taskExecutor") AsyncTaskExecutor taskExecutor, FutureDataPermissionRepository futureDataPermissionRepository, PermissionRequestRepository<TimeframedPermissionRequest> permissionRequestRepository, Sinks.Many<ConnectionStatusMessage> connectionStatusSink) {
        this.pollingService = pollingService;
        this.taskExecutor = taskExecutor;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
        this.permissionRequestRepository = permissionRequestRepository;
        this.connectionStatusSink = connectionStatusSink;
    }

    public void terminateFutureDataPermission(TimeframedPermissionRequest permissionRequest) {
        var futureDataPermission = futureDataPermissionRepository.findFutureDataPermissionByPermissionId(permissionRequest.permissionId());
        futureDataPermission.withState(permissionRequest.state());
        futureDataPermissionRepository.saveAndFlush(futureDataPermission);
    }

    @Scheduled(cron = "${region-connector.fr.enedis.tasks.cron.future.data.permission.poll}")
    private void pollFutureData() {
        List<FutureDataPermission> futureDataPermissions = getValidFutureDataPermissions(ZonedDateTime.now(ZONE_ID_FR).toInstant());
        for (int i = 0; i < futureDataPermissions.size(); i += tasksPerThread) {
            int end = Math.min(i + tasksPerThread, futureDataPermissions.size());
            List<FutureDataPermission> batch = futureDataPermissions.subList(i, end);

            taskExecutor.execute(new PollFutureDataTask(pollingService, batch, futureDataPermissionRepository));
        }
    }

    @Scheduled(cron = "${region-connector.fr.enedis.tasks.cron.future.data.permission.clean}")
    private void removeInvalidFutureDataPermissions() {
        List<FutureDataPermission> futureDataPermissions = getInvalidFutureDataPermissions(ZonedDateTime.now(ZONE_ID_FR).toInstant());

        for (int i = 0; i < futureDataPermissions.size(); i += tasksPerThread) {
            int end = Math.min(i + tasksPerThread, futureDataPermissions.size());
            List<FutureDataPermission> batch = futureDataPermissions.subList(i, end);
            taskExecutor.execute(new RemoveInvalidFutureDataPermissionTask(batch, futureDataPermissionRepository, permissionRequestRepository, connectionStatusSink));
        }
    }

    private List<FutureDataPermission> getValidFutureDataPermissions(Instant today) {
        return futureDataPermissionRepository.findValidFutureDataPermissions(today, ACCEPTED);
    }

    private List<FutureDataPermission> getInvalidFutureDataPermissions(Instant today) {
        return futureDataPermissionRepository.findInvalidFutureDataPermissions(today, ACCEPTED);
    }
}
