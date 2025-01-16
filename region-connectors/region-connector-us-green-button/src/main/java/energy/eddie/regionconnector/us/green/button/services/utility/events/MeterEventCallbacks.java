package energy.eddie.regionconnector.us.green.button.services.utility.events;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.client.dtos.meter.Meter;
import energy.eddie.regionconnector.us.green.button.dtos.WebhookEvent;
import energy.eddie.regionconnector.us.green.button.permission.events.PollingStatus;
import energy.eddie.regionconnector.us.green.button.permission.events.UsStartPollingEvent;
import energy.eddie.regionconnector.us.green.button.permission.request.api.UsGreenButtonPermissionRequest;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReading;
import energy.eddie.regionconnector.us.green.button.permission.request.meter.reading.MeterReadingPk;
import energy.eddie.regionconnector.us.green.button.persistence.MeterReadingRepository;
import energy.eddie.regionconnector.us.green.button.services.historical.collection.DataNeedMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class MeterEventCallbacks {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventCallbacks.class);
    private final MeterReadingRepository readingRepository;
    private final Outbox outbox;
    private final GreenButtonApi api;
    private final DataNeedMatcher dataNeedMatcher;

    public MeterEventCallbacks(
            MeterReadingRepository readingRepository,
            Outbox outbox,
            GreenButtonApi api,
            DataNeedMatcher dataNeedMatcher
    ) {
        this.readingRepository = readingRepository;
        this.outbox = outbox;
        this.api = api;
        this.dataNeedMatcher = dataNeedMatcher;
    }

    public void onHistoricalCollectionFinishedEvent(
            WebhookEvent event,
            UsGreenButtonPermissionRequest permissionRequest
    ) {
        var permissionId = permissionRequest.permissionId();
        onMeter(event, permissionRequest)
                .subscribe(meter -> {
                               upsertMeter(permissionRequest, meter, PollingStatus.DATA_READY);
                               var meters = readingRepository.findAllByPermissionId(permissionId);
                               if (!meters.isEmpty() && meters.stream().allMatch(MeterReading::isReadyToPoll)) {
                                   LOGGER.info("Historical collection for permission request {} finished", permissionId);
                                   outbox.commit(new UsStartPollingEvent(permissionId));
                               }
                           },
                           throwable -> onError(event, throwable, permissionId));
    }

    public void onMeterCreatedEvent(WebhookEvent event, UsGreenButtonPermissionRequest permissionRequest) {
        onMeter(event, permissionRequest)
                .subscribe(
                        meter -> upsertMeter(permissionRequest, meter, PollingStatus.DATA_NOT_READY),
                        throwable -> onError(event, throwable, permissionRequest.permissionId())
                );
    }

    private Mono<Meter> onMeter(WebhookEvent event, UsGreenButtonPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        var meterUid = event.meterUid();
        if (meterUid == null) {
            LOGGER.warn("Got event {} for permission request {} without meter_uid",
                        event.type(),
                        permissionId);
            return Mono.empty();
        }
        if (permissionRequest.status() != PermissionProcessStatus.ACCEPTED) {
            LOGGER.info("Got event {} for {} permission request {}",
                        event.type(),
                        permissionRequest.status(),
                        permissionId);
            return Mono.empty();
        }
        return api.fetchMeter(meterUid, permissionRequest.dataSourceInformation().meteredDataAdministratorId())
                  .filter(meter -> dataNeedMatcher.isRelevantEnergyType(meter, permissionRequest))
                  .doOnSuccess(meter -> {
                      if (LOGGER.isInfoEnabled() && !permissionRequest.allowedMeters().contains(meterUid)) {
                          LOGGER.info("Found meter {}, adding to permission request {}", meterUid, permissionId);
                      }
                  });
    }

    private void upsertMeter(
            UsGreenButtonPermissionRequest permissionRequest,
            Meter meter,
            PollingStatus pollingStatus
    ) {
        var permissionId = permissionRequest.permissionId();
        var meterUid = meter.uid();
        if (readingRepository.existsById(new MeterReadingPk(permissionId, meterUid))) {
            LOGGER.info("Updating meter {} of permission request {} to be {}", meterUid, permissionId, pollingStatus);
            readingRepository.updateHistoricalCollectionStatusForMeter(pollingStatus, permissionId, meterUid);
        } else {
            LOGGER.info("Adding meter {} of permission request {} to be {}", meterUid, permissionId, pollingStatus);
            readingRepository.save(new MeterReading(permissionId, meterUid, null, pollingStatus));
        }
    }

    private void onError(WebhookEvent event, Throwable throwable, String permissionId) {
        if (throwable instanceof WebClientResponseException webClientResponseException) {
            LOGGER.info(
                    "Meter {} of historical collection finished event for permission request {} results in exception, with http status {}",
                    event.meterUid(),
                    permissionId,
                    webClientResponseException.getStatusCode(),
                    throwable
            );
        } else {
            LOGGER.info(
                    "Meter {} of historical collection finished event for permission request {} results in exception",
                    event.meterUid(),
                    permissionId,
                    throwable
            );
        }
    }
}
