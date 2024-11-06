package energy.eddie.regionconnector.us.green.button.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.*;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtilityEventService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilityEventService.class);
    private final Outbox outbox;
    private final UsPermissionRequestRepository repository;
    private final MeterReadingRepository readingRepository;

    public UtilityEventService(
            Outbox outbox,
            UsPermissionRequestRepository repository,
            MeterReadingRepository readingRepository
    ) {
        this.outbox = outbox;
        this.repository = repository;
        this.readingRepository = readingRepository;
    }

    public void receiveEvents(List<WebhookEvent> events) throws PermissionNotFoundException {
        for (var event : events) {
            receiveEvent(event);
        }
    }

    private void receiveEvent(WebhookEvent event) throws PermissionNotFoundException {
        if (event.authorizationUid() == null) {
            LOGGER.info("Got event {} without authorization UID", event.type());
            return;
        }
        var res = repository.findByAuthUid(event.authorizationUid());
        if (res == null) {
            throw new PermissionNotFoundException("unknown");
        }
        var permissionId = res.permissionId();
        LOGGER.info("Got webhook event '{}' for permission request {}", event.type(), permissionId);
        switch (event.type()) {
            case "authorization_expired":
                if (res.status() != PermissionProcessStatus.EXTERNALLY_TERMINATED)
                    outbox.commit(new UsUnfulfillableEvent(permissionId, false));
                break;
            case "authorization_revoked":
                outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
                break;
            case "meter_historical_collection_finished_successful":
                var meterUid = event.meterUid();
                if (meterUid == null) {
                    LOGGER.warn("Got event {} for permission request {} without meter_uid",
                                event.type(),
                                permissionId);
                    break;
                }
                if (res.status() != PermissionProcessStatus.ACCEPTED) {
                    LOGGER.info("Got event {} for not accepted permission request {}", event.type(), permissionId);
                    break;
                }
                LOGGER.info("Historical collection finished for meter {} for permission request {}",
                            meterUid,
                            permissionId);
                readingRepository.updateHistoricalCollectionStatusForMeter(PollingStatus.DATA_READY,
                                                                           permissionId,
                                                                           meterUid);
                var meters = readingRepository.findAllByPermissionId(permissionId);
                if (meters.stream().allMatch(MeterReading::isReadyToPoll)) {
                    LOGGER.info("Historical collection for permission request {} finished", permissionId);
                    outbox.commit(new UsStartPollingEvent(permissionId));
                }
                break;
            default:
                // No-Op
        }
    }
}
