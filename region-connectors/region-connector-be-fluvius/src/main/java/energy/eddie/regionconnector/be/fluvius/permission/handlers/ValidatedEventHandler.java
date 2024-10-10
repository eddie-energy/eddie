package energy.eddie.regionconnector.be.fluvius.permission.handlers;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.permission.events.InvalidEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.events.ValidatedEvent;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class ValidatedEventHandler implements EventHandler<ValidatedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ValidatedEventHandler.class);
    private final FluviusApi fluviusApi;
    private final BePermissionRequestRepository bePermissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> dataNeedCalculationService;
    private final Outbox outbox;

    public ValidatedEventHandler(
            EventBus eventBus, FluviusApi fluviusApi,
            BePermissionRequestRepository bePermissionRequestRepository,
            DataNeedCalculationService<DataNeed> dataNeedCalculationService,
            Outbox outbox
    ) {
        eventBus.filteredFlux(ValidatedEvent.class).subscribe(this::accept);
        this.fluviusApi = fluviusApi;
        this.bePermissionRequestRepository = bePermissionRequestRepository;
        this.dataNeedCalculationService = dataNeedCalculationService;
        this.outbox = outbox;
    }

    @Override
    public void accept(ValidatedEvent event) {
        var permissionId = event.permissionId();
        var pr = bePermissionRequestRepository.getByPermissionId(permissionId);
        var dataNeed = dataNeedCalculationService.calculate(pr.dataNeedId());
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeedResult vhdDataNeed)) {
            return;
        }
        var from = vhdDataNeed.energyTimeframe().start().atStartOfDay(ZoneOffset.UTC);
        if (from.isAfter(ZonedDateTime.now(ZoneOffset.UTC))) {
            from = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        }
        var end = DateTimeUtils.endOfDay(vhdDataNeed.energyTimeframe().end(), ZoneOffset.UTC);
        LOGGER.info("Sending permission request {} to fluvius from {} to {}", permissionId, from, end);
        fluviusApi.shortUrlIdentifier(permissionId, event.flow(), from, end)
                  .subscribe(res -> handleSuccess(permissionId, res),
                             throwable -> handleError(throwable, event));
    }

    private void handleSuccess(String permissionId, FluviusSessionCreateResultResponseModelApiDataResponse res) {
        LOGGER.info("Successfully sent permission request {} to Fluvius", permissionId);
        if (res.getData() == null) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new InvalidEvent(permissionId, "No short url identifier found"));
        } else {
            outbox.commit(new SentToPaEvent(permissionId, res.getData().getShortUrlIdentifier()));
        }
    }

    private void handleError(Throwable throwable, PermissionEvent event) {
        var permissionId = event.permissionId();
        switch (throwable) {
            case WebClientResponseException e when e.getStatusCode().is4xxClientError() -> {
                LOGGER.warn("Invalid permission request {}", permissionId, e);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
                outbox.commit(new InvalidEvent(permissionId, e.getResponseBodyAsString(StandardCharsets.UTF_8)));
            }
            default -> {
                LOGGER.warn("Could not send permission request {} to fluvius", permissionId, throwable);
                outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
            }
        }
    }
}
