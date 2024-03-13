package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.ZonedDateTime;
import java.util.Optional;

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
        var permissionId = permissionRequest.permissionId();
        ZonedDateTime meteringDataDate = identifiableMeteringData.intermediateMeteringData().end();

        if (isLatestMeterReading(permissionRequest, meteringDataDate)) {
            LOGGER.info("Updating latest meter reading for permission request {} from {} to {}", permissionId, permissionRequest.lastPulledMeterReading(), meteringDataDate);
            permissionRequest.setLastPulledMeterReading(identifiableMeteringData.intermediateMeteringData().end());

            if (isFulfilled(permissionRequest, meteringDataDate)) {
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

    private static boolean isLatestMeterReading(EsPermissionRequest permissionRequest, ZonedDateTime meteringDataDate) {
        return permissionRequest
                .lastPulledMeterReading()
                .map(meteringDataDate::isAfter)
                .orElse(true);
    }

    /**
     * Checks if the permission request is fulfilled.
     *
     * @param permissionRequest   the permission request
     * @param meterReadingEndDate the end date of the meter reading
     * @return true if {@code meterReadingEndDate} is >= {@link EsPermissionRequest#end()}. The {@link EsPermissionRequest#end()} is already inclusive, so we check >= instead of >.
     */
    private static boolean isFulfilled(EsPermissionRequest permissionRequest, ZonedDateTime meterReadingEndDate) {
        return Optional.ofNullable(permissionRequest.end())
                .map(ZonedDateTime::toLocalDate)
                .map(end -> !meterReadingEndDate.toLocalDate().isBefore(end))
                .orElse(false);
    }
}
