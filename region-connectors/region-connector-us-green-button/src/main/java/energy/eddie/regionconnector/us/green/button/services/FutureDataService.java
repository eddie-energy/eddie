package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.HistoricalCollectionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

@Service
public class FutureDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureDataService.class);
    private final PermissionRequestService permissionRequestService;
    private final HistoricalCollectionService historicalCollectionService;
    private final MeterReadingRepository meterReadingRepository;

    public FutureDataService(
            PermissionRequestService permissionRequestService,
            HistoricalCollectionService historicalCollectionService,
            MeterReadingRepository meterReadingRepository
    ) {
        this.permissionRequestService = permissionRequestService;
        this.historicalCollectionService = historicalCollectionService;
        this.meterReadingRepository = meterReadingRepository;
    }

    @Scheduled(cron = "${region-connector.us.green.button.polling:0 0 17 * * *}")
    @Transactional(Transactional.TxType.REQUIRED)
    public void pollFutureData() {
        LOGGER.info("Starting polling for future data");
        var activePermissionRequests = permissionRequestService.findActivePermissionRequests();
        if (activePermissionRequests.isEmpty()) {
            LOGGER.info("No active permission requests found");
            return;
        }
        var meterReadings = activePermissionRequests.stream()
                                                    .flatMap(pr -> pr.lastMeterReadings().stream())
                                                    .toList();
        LOGGER.info("Triggering historical collection for future data for meters {}", meterReadings);
        for (var meterReading : meterReadings) {
            meterReadingRepository.updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_NOT_READY,
                                                                            meterReading.permissionId(),
                                                                            meterReading.meterUid());
        }
        var permissionIds = activePermissionRequests.stream()
                                                    .map(PermissionRequest::permissionId)
                                                    .toList();
        historicalCollectionService.triggerHistoricalDataCollection(meterReadings)
                                   .publishOn(Schedulers.boundedElastic())
                                   .doFinally(meters -> permissionRequestService.removeUnfulfillablePermissionRequests(
                                           permissionIds
                                   ))
                                   .subscribe();
    }
}
