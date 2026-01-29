package energy.eddie.regionconnector.de.eta.service;

import energy.eddie.regionconnector.de.eta.permission.request.events.LatestMeterReadingEvent;
import energy.eddie.regionconnector.de.eta.providers.IdentifiableValidatedHistoricalData;
import energy.eddie.regionconnector.de.eta.providers.ValidatedHistoricalDataStream;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Service responsible for handling meter reading updates.
 * Subscribes to the ValidatedHistoricalDataStream and emits LatestMeterReadingEvents
 * to track the latest meter reading date for each permission request.
 */
@Service
public class MeterReadingUpdateService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterReadingUpdateService.class);

    private final Outbox outbox;

    public MeterReadingUpdateService(ValidatedHistoricalDataStream stream, Outbox outbox) {
        this.outbox = outbox;
        stream.validatedHistoricalData()
              .subscribe(this::handleMeterReading);
    }

    private void handleMeterReading(IdentifiableValidatedHistoricalData data) {
        var permissionRequest = data.permissionRequest();
        LocalDate endDate = data.payload().endDate();
        
        if (endDate == null) {
            LOGGER.warn("No end date available for permission request {}", permissionRequest.permissionId());
            return;
        }
        
        ZonedDateTime latestReading = endDate.atStartOfDay(ZoneId.of("UTC"));

        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .addArgument(() -> latestReading)
              .log("Updating latest meter reading for permission request {} to {}");

        outbox.commit(new LatestMeterReadingEvent(
                permissionRequest.permissionId(), latestReading
        ));
    }
}

