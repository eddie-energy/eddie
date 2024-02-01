package energy.eddie.regionconnector.fr.enedis.tasks;

import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import energy.eddie.regionconnector.fr.enedis.services.PollingService;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

public class FutureDataPermissionPollTask implements Runnable {
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Paris");
    private final PollingService pollingService;
    private final List<FutureDataPermission> futureDataPermissions;
    private final FutureDataPermissionRepository futureDataPermissionRepository;

    public FutureDataPermissionPollTask(PollingService pollingService, List<FutureDataPermission> futureDataPermissions, FutureDataPermissionRepository futureDataPermissionRepository) {
        this.pollingService = pollingService;
        this.futureDataPermissions = futureDataPermissions;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
    }

    private void pollFutureData() {
        for (var futureDataPermission : futureDataPermissions) {
            // Copy permission request to retrieve the data of yesterday
            // ENEDIS only allows to retrieve the values from the day before, we have to subtract 1 from the current date and 2 from the start date
            var today = ZonedDateTime.now(ZONE_ID);
            var end = today.minusDays(1);
            var start = today.minusDays(2);
            var permissionRequestForToday = new FutureDataPermission(futureDataPermission);
            permissionRequestForToday.withValidFrom(start).withValidTo(end);

            // Retrieve the data
            pollingService.requestData(permissionRequestForToday, futureDataPermission.getMeteringPointId());

            // Set the old values back and update the last poll column
            futureDataPermission.setLastPoll(today);
        }

        futureDataPermissionRepository.saveAll(futureDataPermissions);
    }

    @Override
    public void run() {
        pollFutureData();
    }
}
