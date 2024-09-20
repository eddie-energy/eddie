package energy.eddie.regionconnector.us.green.button.permission.handlers;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.shared.event.sourcing.EventBus;
import energy.eddie.regionconnector.shared.event.sourcing.handlers.EventHandler;
import energy.eddie.regionconnector.us.green.button.api.GreenButtonApi;
import energy.eddie.regionconnector.us.green.button.api.Pages;
import energy.eddie.regionconnector.us.green.button.client.dtos.Meter;
import energy.eddie.regionconnector.us.green.button.client.dtos.MeterListing;
import energy.eddie.regionconnector.us.green.button.config.GreenButtonConfiguration;
import energy.eddie.regionconnector.us.green.button.oauth.persistence.OAuthTokenRepository;
import energy.eddie.regionconnector.us.green.button.permission.events.UsSimpleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class AcceptedHandler implements EventHandler<List<UsSimpleEvent>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AcceptedHandler.class);
    private final GreenButtonApi api;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final boolean requiresPagination;

    public AcceptedHandler(
            EventBus eventBus,
            GreenButtonApi api,
            OAuthTokenRepository oAuthTokenRepository,
            GreenButtonConfiguration config
    ) {
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.api = api;
        // Pagination is not required if the batch size is smaller-equals the maximum amount of meters.
        requiresPagination = config.activationBatchSize() > GreenButtonApi.MAX_METER_RESULTS;
        eventBus.filteredFlux(PermissionProcessStatus.ACCEPTED)
                .ofType(UsSimpleEvent.class)
                .buffer(config.activationBatchSize())
                .subscribe(this::accept);
    }

    @Override
    public void accept(List<UsSimpleEvent> events) {
        var ids = events.stream().map(PermissionEvent::permissionId).toList();
        var permissionAuthIds = oAuthTokenRepository.findAllByPermissionIdIn(ids);
        var authIds = permissionAuthIds.stream().map(OAuthTokenRepository.PermissionAuthId::getAuthUid).toList();
        var page = requiresPagination ? Pages.SLURP : Pages.NO_SLURP;
        api.fetchInactiveMeters(page, authIds)
           .flatMap(AcceptedHandler::meterIdStream)
           .collectList()
           .flatMap(api::collectHistoricalData)
           .subscribe(response -> LOGGER.atInfo()
                                        .addArgument(response::meters)
                                        .log("Started collection of historical data for meters: {}"));
    }

    private static Flux<String> meterIdStream(MeterListing listing) {
        return Flux.fromStream(
                listing.meters()
                       .stream()
                       .map(Meter::uid)
        );
    }
}
