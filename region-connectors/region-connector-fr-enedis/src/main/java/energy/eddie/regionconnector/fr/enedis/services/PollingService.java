package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import energy.eddie.regionconnector.fr.enedis.model.ConsumptionLoadCurveMeterReading;
import energy.eddie.regionconnector.fr.enedis.providers.agnostic.IdentifiableMeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

@Service
public class PollingService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private static final int NUM_OF_WEEKDAYS = 6;

    private final EnedisApiService enedisApiService;
    private final Sinks.Many<IdentifiableMeterReading> meterReadings;

    public PollingService(EnedisApiService enedisApiService, Sinks.Many<IdentifiableMeterReading> meterReadings) {
        this.enedisApiService = enedisApiService;
        this.meterReadings = meterReadings;
    }

    private static void handleException(TimeframedPermissionRequest permissionRequest, ApiException e) {
        if (e.getCode() != HttpStatus.FORBIDDEN.value()) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Something went wrong while fetching token or data from ENEDIS for permission request id: {}", permissionRequest.permissionId(), e);
            }
            return;
        }
        try {
            permissionRequest.revoke();
        } catch (StateTransitionException ex) {
            LOGGER.warn("Unable to revoke permission request", ex);
        }
    }

    @Async
    public void requestData(TimeframedPermissionRequest permissionRequest, String usagePointId) {
        try {
            fetchConsumptionRecords(permissionRequest, usagePointId);
        } catch (ApiException e) {
            handleException(permissionRequest, e);
        }
    }

    private void fetchConsumptionRecords(TimeframedPermissionRequest permissionRequest, String usagePointId) throws ApiException {
        // request data from enedis
        ZonedDateTime end = permissionRequest.end();
        String permissionId = permissionRequest.permissionId();
        // the api allows for a maximum of 7 days per request, so we need to split the request
        for (ZonedDateTime start = permissionRequest.start(); start.isBefore(permissionRequest.end()); start = start.plusDays(NUM_OF_WEEKDAYS)) {
            ZonedDateTime endOfRequest = start.plusDays(NUM_OF_WEEKDAYS); // including the start date, so 7 days
            if (endOfRequest.isAfter(end)) {
                endOfRequest = end;
            }
            LOGGER.info("Fetching data from ENEDIS for permissionId '{}' from '{}' to '{}'", permissionId, start, endOfRequest);
            ConsumptionLoadCurveMeterReading meterReading = enedisApiService.getConsumptionLoadCurve(usagePointId, start, endOfRequest);
            // publish
            meterReadings.tryEmitNext(new IdentifiableMeterReading(
                    permissionId,
                    permissionRequest.connectionId(),
                    permissionRequest.dataNeedId(),
                    meterReading
            ));
        }
    }

    @Override
    public void close() throws Exception {
        meterReadings.tryEmitComplete();
    }
}