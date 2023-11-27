package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.invoker.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private static final int NUM_OF_WEEKDAYS = 6;

    private final EnedisApiService enedisApiService;
    private final Sinks.Many<ConsumptionRecord> consumptionRecords;

    public PollingService(EnedisApiService enedisApiService, Sinks.Many<ConsumptionRecord> consumptionRecords) {
        this.enedisApiService = enedisApiService;
        this.consumptionRecords = consumptionRecords;
    }

    @Async
    public void requestData(TimeframedPermissionRequest permissionRequest, String usagePointId) {
        try {
            fetchConsumptionRecords(permissionRequest, usagePointId);
        } catch (ApiException e) {
            LOGGER.error("Something went wrong while fetching token or data from ENEDIS for permission request id: {}", permissionRequest.permissionId(), e);
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
            ConsumptionRecord consumptionRecord = enedisApiService.getConsumptionLoadCurve(usagePointId, start, endOfRequest);
            // map ids
            consumptionRecord.setConnectionId(permissionRequest.connectionId());
            consumptionRecord.setPermissionId(permissionId);
            consumptionRecord.setDataNeedId(permissionRequest.dataNeedId());
            // publish
            consumptionRecords.tryEmitNext(consumptionRecord);
        }
    }
}
