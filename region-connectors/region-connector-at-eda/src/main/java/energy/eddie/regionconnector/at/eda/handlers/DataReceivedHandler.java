package energy.eddie.regionconnector.at.eda.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.permission.request.events.DataReceivedEvent;
import energy.eddie.regionconnector.at.eda.persistence.MeterReadingTimeframeRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.isBeforeOrEquals;

@Component
public class DataReceivedHandler implements EventHandler<DataReceivedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataReceivedHandler.class);
    private static final Set<PermissionProcessStatus> STATUSES_AFTER_ACCEPTED = Collections.unmodifiableSet(EnumSet.of(
            PermissionProcessStatus.TERMINATED,
            PermissionProcessStatus.REVOKED,
            PermissionProcessStatus.FULFILLED,
            PermissionProcessStatus.INVALID,
            PermissionProcessStatus.MALFORMED,
            PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION,
            PermissionProcessStatus.FAILED_TO_TERMINATE,
            PermissionProcessStatus.EXTERNALLY_TERMINATED));
    private final FulfillmentService fulfillmentService;
    private final AtPermissionRequestRepository repository;
    private final MeterReadingTimeframeRepository timeframeRepository;

    public DataReceivedHandler(
            FulfillmentService fulfillmentService,
            EventBus eventBus,
            AtPermissionRequestRepository repository,
            MeterReadingTimeframeRepository timeframeRepository
    ) {
        this.fulfillmentService = fulfillmentService;
        this.repository = repository;
        this.timeframeRepository = timeframeRepository;
        eventBus.filteredFlux(DataReceivedEvent.class).subscribe(this::accept);
    }

    @Override
    public void accept(DataReceivedEvent event) {
        if (isStatusAfterAccepted(event.status())) {
            return;
        }
        var permissionId = event.permissionId();
        var pr = repository.getByPermissionId(permissionId);

        LOGGER.atDebug()
              .addArgument(permissionId)
              .addArgument(pr::end)
              .addArgument(event::end)
              .log("Checking if permission request {} is fulfilled. Permission end date: {}, metering period end date: {}");
        var timeframes = timeframeRepository.findAllByPermissionId(permissionId);
        // A finished permission request requires to be one continuous timeframe
        if (timeframes.size() != 1) {
            return;
        }
        var timeframe = timeframes.getFirst();
        // if we request quarter hourly data up to the 24.01.2024, the last consumption record we get will have a meteringPeriodStart of 24.01.2024T23:45:00 and a meteringPeriodEnd of 25.01.2024T00:00:00
        // so if the permissionEnd is before the meteringPeriodEnd the permission request is fulfilled
        if (isBeforeOrEquals(timeframe.start(), pr.start()) && timeframe.end().isAfter(pr.end())) {
            fulfillmentService.tryFulfillPermissionRequest(pr);
        }
    }

    private boolean isStatusAfterAccepted(PermissionProcessStatus status) {
        return STATUSES_AFTER_ACCEPTED.contains(status);
    }
}
