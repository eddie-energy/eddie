package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;

/**
 * This service updates the last pulled meter reading and checks for fulfillment of the permission request.
 */
@Service
public class IdentifiableMeteringDataService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMeteringDataService.class);

    @SuppressWarnings("java:S1118") // sonar complains that it should be a utility class
    public IdentifiableMeteringDataService(Flux<IdentifiableMeteringData> meteringDataFlux) {
        meteringDataFlux.subscribe(IdentifiableMeteringDataService::updateLastPulledMeterReadingAndCheckForFulfillment);
    }

    private static void updateLastPulledMeterReadingAndCheckForFulfillment(IdentifiableMeteringData identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();
        var lastMeterReading = identifiableMeteringData.meteringData().getLast();
        ZonedDateTime meteringDataDate = lastMeterReading.dateTime();

        updateLastPulledMeterReading(permissionRequest, meteringDataDate);
        checkForFulfillment(permissionRequest, meteringDataDate);
    }

    private static void checkForFulfillment(EsPermissionRequest permissionRequest, ZonedDateTime meteringDataDate) {
        var permissionEnd = permissionRequest.end();
        if (permissionEnd == null) {
            return;
        }

        // the last metring data date is always at 00:00 the next day, so if we get data for the 24.01.2024 the last metering data date will be 25.01.2024T00:00:00
        if (permissionEnd.toLocalDate().isBefore(meteringDataDate.toLocalDate())) {
            try {
                permissionRequest.fulfill();
            } catch (StateTransitionException e) {
                LOGGER.error("Error while fulfilling permission request", e);
            }
        }
    }

    private static void updateLastPulledMeterReading(EsPermissionRequest permissionRequest, ZonedDateTime meteringDataDate) {
        permissionRequest.lastPulledMeterReading().ifPresentOrElse(
                lastPulledMeterReading -> {
                    if (meteringDataDate.isAfter(lastPulledMeterReading)) {
                        permissionRequest.setLastPulledMeterReading(meteringDataDate);
                    }
                },
                () -> permissionRequest.setLastPulledMeterReading(meteringDataDate)
        );
    }
}