package energy.eddie.regionconnector.de.eta.permission.handlers;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.de.eta.client.DeEtaPaApiClient;
import energy.eddie.regionconnector.de.eta.permission.events.RetryValidatedEvent;
import energy.eddie.regionconnector.de.eta.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.de.eta.permission.events.UnableToSendEvent;
import energy.eddie.regionconnector.de.eta.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.de.eta.persistence.DeEtaPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import energy.eddie.regionconnector.de.eta.client.TransientPaException;

@Component
@SuppressWarnings("NullAway")
public class ValidatedEventHandler implements EventHandler<ValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedEventHandler.class);
    private static final String TAG_KEY_REGION = "region";
    private static final String REGION_DE_ETA = "de-eta";
    private static final Tags METRIC_TAGS = Tags.of(TAG_KEY_REGION, REGION_DE_ETA);
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
                                Metrics.counter("validated.sentToPa", METRIC_TAGS).increment();
                                outbox.commit(new SentToPaEvent(permissionId));
                            } else {
                                var reason = "PA response indicates failure or empty response";
                                Metrics.counter("validated.unable", METRIC_TAGS.and("cause", "pa_response")).increment();
                                outbox.commit(new UnableToSendEvent(
                                        permissionId,
                                        pr.connectionId(),
                                        pr.dataNeedId(),
                                        reason
                                ));
                            }
                        },
                        throwable -> {
                            if (throwable instanceof TransientPaException) {
                                // Transient network or server issue: schedule retry according to project pattern
                                LOGGER.warn("Transient error while sending permission request {} to DE-ETA. Scheduling retry. Reason: {}",
                                        permissionId, truncate(throwable.getMessage()));
                                Metrics.counter("validated.retry", METRIC_TAGS).increment();
                                outbox.commit(new RetryValidatedEvent(permissionId));
                            } else {
                                // Non-transient: emit UnableToSendEvent with short reason
                                var reason = buildReasonFromException(throwable);
                                LOGGER.warn("Could not send permission request {} to DE-ETA. Reason: {}", permissionId, reason);
                                Metrics.counter("validated.unable", METRIC_TAGS.and("cause", "exception")).increment();
                                outbox.commit(new UnableToSendEvent(
                                        permissionId,
                                        pr.connectionId(),
                                        pr.dataNeedId(),
                                        reason
                                ));
                            }
                        });
    }

    private void onRetry(RetryValidatedEvent event) {
        // On retry we simply re-use the same sending logic
        accept(new ValidatedEvent(event.permissionId(), null, null, null, null, null));
    }

    private static String buildReasonFromException(Throwable t) {
        try {
            if (t instanceof WebClientResponseException wcre) {
                var status = wcre.getStatusCode();
                var body = wcre.getResponseBodyAsString();
                return formatReason(status.value(), wcre.getStatusText(), body);
            }
        } catch (Exception ignored) {
            // fall through to generic message
        }
        var msg = t.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = t.getClass().getSimpleName();
        }
        return truncate("Exception while sending to PA: " + msg);
    }

    private static String formatReason(int statusCode, String reasonPhrase, String body) {
        String snippet = body == null ? "" : body.replace("\n", " ");
        if (snippet.length() > 500) {
            snippet = snippet.substring(0, 500);
        }
        var base = "HTTP " + statusCode + " " + (reasonPhrase == null ? "" : reasonPhrase);
        if (snippet.isBlank()) {
            return truncate(base);
        }
        return truncate(base + "; body: " + snippet);
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }
}
