package energy.eddie.regionconnector.at.eda.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.at.eda.dto.IdentifiableConsumptionRecord;
import energy.eddie.regionconnector.at.eda.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class PermissionRequestFulfillmentService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestFulfillmentService.class);
    private final Outbox outbox;
    private final FulfillmentService fulfillmentService;

    public PermissionRequestFulfillmentService(
            Flux<IdentifiableConsumptionRecord> consumptionRecordStream,
            Outbox outbox,
            FulfillmentService fulfillmentService
    ) {
        this.outbox = outbox;
        this.fulfillmentService = fulfillmentService;
        consumptionRecordStream.subscribe(this::checkForFulfillment);
    }

    private void checkForFulfillment(IdentifiableConsumptionRecord identifiableConsumptionRecord) {
        identifiableConsumptionRecord.permissionRequests().forEach(permissionRequest -> {
            if (isTerminalState(permissionRequest.status())) {
                return;
            }

            LOGGER.atDebug()
                  .addArgument(permissionRequest::permissionId)
                  .addArgument(permissionRequest::end)
                  .addArgument(identifiableConsumptionRecord::meterReadingEndDate)
                  .log("Checking if permission request {} is fulfilled. Permission end date: {}, metering period end date: {}");

            // if we request quarter hourly data up to the 24.01.2024, the last consumption record we get will have an meteringPeriodStart of 24.01.2024T23:45:00 and an meteringPeriodEnd of 25.01.2024T00:00:00
            // so if the permissionEnd is before the meteringPeriodEnd the permission request is fulfilled
            if (fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest,
                                                                      identifiableConsumptionRecord.meterReadingEndDate())) {
                outbox.commit(new SimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.FULFILLED));
            }
        });
    }

    private boolean isTerminalState(PermissionProcessStatus status) {
        return status == PermissionProcessStatus.TERMINATED
                || status == PermissionProcessStatus.REVOKED
                || status == PermissionProcessStatus.FULFILLED
                || status == PermissionProcessStatus.INVALID
                || status == PermissionProcessStatus.MALFORMED;
    }
}
