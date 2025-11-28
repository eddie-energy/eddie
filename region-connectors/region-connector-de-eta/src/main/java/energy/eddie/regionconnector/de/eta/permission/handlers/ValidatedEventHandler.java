package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.de.eta.client.DeEtaPaApiClient;
import energy.eddie.regionconnector.de.eta.permission.events.RetryValidatedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.de.eta.permission.events.SimpleEvent;
import energy.eddie.regionconnector.de.eta.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
@SuppressWarnings("NullAway")
public class ValidatedEventHandler implements EventHandler<ValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedEventHandler.class);
    private final DeEtaPaApiClient apiClient;
    private final DeEtaPermissionRequestRepository repository;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final Outbox outbox;

    public ValidatedEventHandler(
            EventBus eventBus,
            DeEtaPaApiClient apiClient,
            DeEtaPermissionRequestRepository repository,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            Outbox outbox
    ) {
        this.apiClient = apiClient;
        this.repository = repository;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.outbox = outbox;
        eventBus.filteredFlux(ValidatedEvent.class).subscribe(this::accept);
        eventBus.filteredFlux(RetryValidatedEvent.class).subscribe(this::onRetry);
    }

    @Override
    public void accept(ValidatedEvent event) {
        var permissionId = event.permissionId();
        var pr = repository.getByPermissionId(permissionId);

        // calculate timeframe if necessary (historical data)
        var dataNeed = dataNeedCalculationService.calculate(pr.dataNeedId(), pr.created());
        if (dataNeed instanceof ValidatedHistoricalDataDataNeedResult vhdDataNeed) {
            var from = vhdDataNeed.permissionTimeframe().start().atStartOfDay(ZoneOffset.UTC);
            var end = DateTimeUtils.endOfDay(vhdDataNeed.permissionTimeframe().end(), ZoneOffset.UTC);
            LOGGER.info("Sending permission request {} to DE-ETA from {} to {}", permissionId, from, end);
        } else {
            LOGGER.info("Sending permission request {} to DE-ETA (no historical timeframe)", permissionId);
        }

        apiClient.sendPermissionRequest(pr)
                .subscribe(resp -> {
                            if (resp != null && resp.success()) {
                                outbox.commit(new SentToPaEvent(permissionId));
                            } else {
                                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                            }
                        },
                        throwable -> {
                            LOGGER.warn("Could not send permission request {} to DE-ETA", permissionId, throwable);
                            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
                        });
    }

    private void onRetry(RetryValidatedEvent event) {
        // On retry we simply re-use the same sending logic
        accept(new ValidatedEvent(event.permissionId(), null, null, null, null, null));
    }
}
