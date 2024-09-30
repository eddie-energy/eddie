package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.permission.events.*;
import energy.eddie.regionconnector.us.green.button.services.DataNeedMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AcceptedHandler implements EventHandler<List<UsAcceptedEvent>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final GreenButtonApi api;
    private final boolean requiresPagination;
    private final DataNeedMatcher dataNeedMatcher;
    private final Outbox outbox;

    public AcceptedHandler(
            EventBus eventBus,
            GreenButtonApi api,
            GreenButtonConfiguration config,
            DataNeedMatcher dataNeedMatcher,
            Outbox outbox
    ) {
        this.api = api;
        // Pagination is not required if the batch size is smaller-equals the maximum amount of meters.
        requiresPagination = config.activationBatchSize() > GreenButtonApi.MAX_METER_RESULTS;
        eventBus.filteredFlux(UsAcceptedEvent.class)
                .buffer(config.activationBatchSize())
                .subscribe(this::accept);
        this.dataNeedMatcher = dataNeedMatcher;
        this.outbox = outbox;
    }

    @Override
    public void accept(List<UsAcceptedEvent> events) {
        var authIds = events.stream().map(UsAcceptedEvent::authUid).toList();
        var authIndex = events
                .stream()
                .collect(Collectors.toMap(UsAcceptedEvent::authUid, PersistablePermissionEvent::permissionId));
        var page = requiresPagination ? Pages.SLURP : Pages.NO_SLURP;
        api.fetchInactiveMeters(page, authIds)
           .map(dataNeedMatcher::filterMetersNotMeetingDataNeedCriteria)
           .flatMap(Flux::fromIterable)
           .collectList()
           .subscribe(meters -> consume(meters, authIndex));
    }

    private void consume(List<Meter> meters, Map<String, String> authIndex) {
        Map<String, List<String>> permissionToMeters = HashMap.newHashMap(authIndex.size());
        for (var permissionId : authIndex.values()) {
            permissionToMeters.put(permissionId, new ArrayList<>());
        }
        for (var meter : meters) {
            var authUid = meter.authorizationUid();
            var permissionId = authIndex.get(authUid);
            var meterUid = meter.uid();
            if (permissionToMeters.containsKey(permissionId))
                permissionToMeters.get(permissionId).add(meterUid);
        }
        api.collectHistoricalData(meters.stream().map(Meter::uid).toList())
           .subscribe(res -> {
               var activatedMeters = res.meters();
               for (var entry : permissionToMeters.entrySet()) {
                   var metersOfPermission = new ArrayList<>(entry.getValue());
                   // Remove all meters that where not activated
                   metersOfPermission.retainAll(activatedMeters);
                   List<MeterReading> meterReadings = new ArrayList<>(metersOfPermission.size());
                   var permissionId = entry.getKey();
                   for (var meter : metersOfPermission) {
                       meterReadings.add(new MeterReading(permissionId, meter, null));
                   }
                   if (metersOfPermission.isEmpty()) {
                       LOGGER.info("Marking permission request {} as unfulfillable, since no meter supports data need",
                                   permissionId);
                       outbox.commit(new UsSimpleEvent(permissionId, PermissionProcessStatus.UNFULFILLABLE));
                   } else {
                       LOGGER.info("Adding meters with UIDs {} to permission request {}",
                                   metersOfPermission,
                                   permissionId);
                       outbox.commit(new UsMeterReadingUpdateEvent(permissionId,
                                                                   meterReadings,
                                                                   PollingStatus.DATA_NOT_READY));
                   }
               }
           });
    }
}
