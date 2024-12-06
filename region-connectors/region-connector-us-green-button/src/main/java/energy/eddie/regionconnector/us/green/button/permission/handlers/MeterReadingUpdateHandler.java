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
        LOGGER.info("Got meter reading update for permission request {}", event.permissionId());
        var latestReading = event.latestMeterReadingEndDateTime();
        if (latestReading.isEmpty()) {
            LOGGER.info("No meter reading update for permission request {}", event.permissionId());
            return;
        }
        var pr = repository.getByPermissionId(event.permissionId());
        var end = DateTimeUtils.endOfDay(pr.end(), ZoneOffset.UTC);
        if (latestReading.get().isAfter(end) || latestReading.get().isEqual(end)) {
            fulfillmentService.tryFulfillPermissionRequest(pr);
        }
    }
}
