package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.fi.fingrid.client.EventReason;
import energy.eddie.regionconnector.fi.fingrid.client.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.events.UpdateGranularityEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
public class UpdateGranularityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateGranularityService.class);
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;

    public UpdateGranularityService(
            Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public Mono<TimeSeriesResponse> updateGranularity(
            TimeSeriesResponse timeSeriesResponse,
            FingridPermissionRequest permissionRequest
    ) {
        var transaction = timeSeriesResponse.data().transaction();
        if (transaction.timeSeries() != null
            || !Objects.equals(transaction.reasonCode(), EventReason.EMPTY_RESPONSE_REASON)) {
            return Mono.just(timeSeriesResponse);
        }
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Retrying permission request {} with higher granularity", permissionId);
        var granularity = findHigherGranularity(permissionRequest);
        if (granularity == null) {
            LOGGER.info("No higher granularity found for permission request {} making it unfulfillable", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
        } else {
            LOGGER.info("Found higher granularity for permission request {}", permissionId);
            outbox.commit(new UpdateGranularityEvent(permissionId, granularity));
        }
        return Mono.empty();
    }

    @Nullable
    private Granularity findHigherGranularity(FingridPermissionRequest permissionRequest) {
        var calc = calculationService.calculate(permissionRequest.dataNeedId());
        if (!(calc instanceof ValidatedHistoricalDataDataNeedResult vhdDataNeed))
            return null;
        for (var granularity : vhdDataNeed.granularities()) {
            if (granularity.minutes() > permissionRequest.granularity().minutes()) {
                return granularity;
            }
        }
        return null;
    }
}
