package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.filter.IdentifiableApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;

@Service
public class PollingService implements AutoCloseable {
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1)).filter(error -> error instanceof WebClientResponseException.TooManyRequests || error instanceof WebClientResponseException.ServiceUnavailable);
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Flux<IdentifiableApiResponse> apiResponseFlux;
    private final Sinks.Many<IdentifiableApiResponse> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final PermissionRequestService permissionRequestService;


    public PollingService(EnerginetCustomerApi energinetCustomerApi,
                          PermissionRequestService permissionRequestService) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.permissionRequestService = permissionRequestService;
        apiResponseFlux = sink.asFlux()
                .share();
    }

    private static boolean isActiveAndNeedsToBePolled(DkEnerginetCustomerPermissionRequest pr) {
        ZonedDateTime now = ZonedDateTime.now(DK_ZONE_ID);
        return pr.lastPolled().toLocalDate().isBefore(now.toLocalDate())
                && (now.isAfter(pr.start()) || now.isEqual(pr.start()))
                && (now.isBefore(pr.end()) || now.isEqual(pr.end()));
    }

    /**
     * Fetches future meter readings for all accepted active permission requests.
     */
    @Scheduled(cron = "${region-connector.dk.energinet.polling:0 0 17 * * *}", zone = "Europe/Copenhagen")
    public void fetchFutureMeterReadings() {

        List<DkEnerginetCustomerPermissionRequest> prs = permissionRequestService.findAllAcceptedPermissionRequests();
        LOGGER.info("Fetching metering data for {} permission requests", prs.size());
        prs
                .stream()
                .filter(PollingService::isActiveAndNeedsToBePolled)
                .forEach(this::fetch);
    }

    /**
     * This will try to fetch historical meter readings for the given permission request.
     * If the permission request is for future data, no data will be fetched.
     *
     * @param permissionRequest for historical validated data
     */
    public void fetchHistoricalMeterReadings(DkEnerginetCustomerPermissionRequest permissionRequest) {
        ZonedDateTime end = permissionRequest.end();
        if (end != null && end.isBefore(ZonedDateTime.now(DK_ZONE_ID))) {
            fetch(permissionRequest);
        }
    }

    private void fetch(DkEnerginetCustomerPermissionRequest permissionRequest) {
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);
        LocalDate now = LocalDate.now(DK_ZONE_ID);

        LocalDate dateFrom = permissionRequest.lastPolled().toLocalDate();
        var endDate = Optional.ofNullable(permissionRequest.end()).flatMap(end -> Optional.of(end.toLocalDate()));
        LocalDate dateTo = endDate.filter(d -> d.isBefore(now)).orElse(now);
        String permissionId = permissionRequest.permissionId();

        LOGGER.info("Fetching metering data from Energinet for permission request {} from {} to {}", permissionId, dateFrom, dateTo);
        permissionRequest.accessToken()
                .flatMap(accessToken -> energinetCustomerApi.getTimeSeries(
                        dateFrom,
                        dateTo,
                        permissionRequest.granularity(),
                        meteringPointsRequest,
                        accessToken,
                        UUID.fromString(permissionId)
                ))
                .retryWhen(RETRY_BACKOFF_SPEC)
                // If we get an 401 Unauthorized error, the refresh token was revoked and the permission request with that
                .doOnError(error -> revokePermissionRequest(permissionRequest, error))
                .mapNotNull(MyEnergyDataMarketDocumentResponseListApiResponse::getResult)
                .flatMap(myEnergyDataMarketDocumentResponses ->
                        new IdentifiableApiResponseFilter(permissionRequest, permissionId, dateFrom, dateTo)
                                .filter(myEnergyDataMarketDocumentResponses))
                .doOnError(error -> LOGGER.error("Something went wrong while fetching data from Energinet:", error))
                .onErrorComplete()
                .subscribe(sink::tryEmitNext);
    }

    private void revokePermissionRequest(DkEnerginetCustomerPermissionRequest permissionRequest,
                                         Throwable error) {
        if (!(error instanceof WebClientResponseException.Unauthorized)) {
            LOGGER.warn("Got error while requesting access token", error);
            return;
        }
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Revoking permission request with permission id {}", permissionRequest.permissionId());
            }
            permissionRequest.revoke();
        } catch (StateTransitionException e) {
            LOGGER.warn("Could not revoke permission request", e);
        }
    }

    public Flux<IdentifiableApiResponse> identifiableMeterReadings() {
        return apiResponseFlux;
    }

    @Override
    public void close() {
        sink.tryEmitComplete();
    }
}