package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * This service updates the latest meter reading and checks for fulfillment of the permission request.
 */
@Service
public class IdentifiableMeterReadingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(IdentifiableMeterReadingService.class);

    @SuppressWarnings("java:S1118") // sonar complains that it should be a utility class
    public IdentifiableMeterReadingService(Flux<IdentifiableMeterReading> meteringDataFlux) {
        meteringDataFlux.subscribe(IdentifiableMeterReadingService::updateLatestMeterReadingAndCheckForFulfillment);
    }

    private static void updateLatestMeterReadingAndCheckForFulfillment(IdentifiableMeterReading identifiableMeteringData) {
        var permissionRequest = identifiableMeteringData.permissionRequest();
        var permissionId = permissionRequest.permissionId();
        LocalDate meterReadingEndDate = identifiableMeteringData.meterReading().end();

        if (isLatestMeterReading(permissionRequest, meterReadingEndDate)) {
            LOGGER.info("Updating latest meter reading for permission request {} from {} to {}", permissionId, permissionRequest.latestMeterReading(), meterReadingEndDate);
            permissionRequest.updateLatestMeterReading(meterReadingEndDate);

            if (isFulfilled(permissionRequest, meterReadingEndDate)) {
                LOGGER.info("Fulfilling permission request {}", permissionId);
                try {
                    permissionRequest.fulfill();
                    LOGGER.info("Permission request {} fulfilled", permissionId);
                } catch (StateTransitionException e) {
                    LOGGER.error("Error while fulfilling permission request {}", permissionId, e);
                }
            }
        }
    }

    private static boolean isLatestMeterReading(FrEnedisPermissionRequest permissionRequest, LocalDate meterReadingEndDate) {
        return permissionRequest
                .latestMeterReading()
                .map(meterReadingEndDate::isAfter)
                .orElse(true);
    }

    /**
     * Checks if the permission request is fulfilled.
     *
     * @param permissionRequest   the permission request
     * @param meterReadingEndDate the end date of the meter reading
     * @return true if {@code meterReadingEndDate} is after {@code permissionRequest.end()}
     */
    private static boolean isFulfilled(FrEnedisPermissionRequest permissionRequest, LocalDate meterReadingEndDate) {
        return meterReadingEndDate.isAfter(permissionRequest.end()); // we also want data for the end date of the permission request, so we use isAfter instead of !isBefore
    }
}
