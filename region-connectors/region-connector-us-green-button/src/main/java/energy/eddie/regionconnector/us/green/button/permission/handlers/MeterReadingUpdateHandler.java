package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import energy.eddie.regionconnector.us.green.button.permission.events.UsMeterReadingUpdateEvent;
import energy.eddie.regionconnector.us.green.button.persistence.UsPermissionRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class MeterReadingUpdateHandler implements EventHandler<UsMeterReadingUpdateEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingUpdateHandler.class);
    private final FulfillmentService fulfillmentService;
    private final UsPermissionRequestRepository repository;

    public MeterReadingUpdateHandler(
            FulfillmentService fulfillmentService, EventBus eventBus,
            UsPermissionRequestRepository repository
    ) {
        this.fulfillmentService = fulfillmentService;
        this.repository = repository;
        eventBus.filteredFlux(UsMeterReadingUpdateEvent.class)
                .subscribe(this::accept);
    }

    @Override
    public void accept(UsMeterReadingUpdateEvent event) {
        var permissionId = event.permissionId();
        LOGGER.info("Got meter reading update for permission request {}", permissionId);
        var latestReading = event.latestMeterReadingEndDateTime();
        if (latestReading.isEmpty()) {
            LOGGER.info("No meter reading update for permission request {}", permissionId);
            return;
        }
        var readingDateTime = latestReading.get();
        LOGGER.info("Latest meter reading is {} for permission request {}", readingDateTime, permissionId);
        var pr = repository.getByPermissionId(permissionId);
        var end = DateTimeUtils.endOfDay(pr.end(), ZoneOffset.UTC);
        if (readingDateTime.isAfter(end) || readingDateTime.isEqual(end)) {
            fulfillmentService.tryFulfillPermissionRequest(pr);
        }
    }
}
