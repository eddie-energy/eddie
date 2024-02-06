package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnector.DK_ZONE_ID;

@Service
public class PollingService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Flux<IdentifiableApiResponse> apiResponseFlux;
    private final Sinks.Many<DkEnerginetCustomerPermissionRequest> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final PermissionRequestService permissionRequestService;


    public PollingService(EnerginetCustomerApi energinetCustomerApi,
                          PermissionRequestService permissionRequestService) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.permissionRequestService = permissionRequestService;
        apiResponseFlux = sink.asFlux()
                .flatMap(this::fetch);
    }

    private static boolean isActive(DkEnerginetCustomerPermissionRequest pr) {
        ZonedDateTime now = ZonedDateTime.now(DK_ZONE_ID);
        return (now.isAfter(pr.start()) || now.isEqual(pr.start()))
                && (now.isBefore(pr.end()) || now.isEqual(pr.end()));
    }

    @Scheduled(cron = "${region-connector.dk.energinet.polling:0 0 17 * * *}")
    public void emitActivePermissionRequests() {
        List<DkEnerginetCustomerPermissionRequest> prs = permissionRequestService.findAllAcceptedPermissionRequests();
        LOGGER.info("Fetching permission requests for future data {}", prs);
        prs
                .stream()
                .filter(PollingService::isActive)
                .forEach(sink::tryEmitNext);
    }

    /**
     * This will try to fetch meter readings and publish it to a flux returned by {@code identifiableMeterReadings}.
     * If the permission request is for future data, no data will be fetched.
     *
     * @param permissionRequest for historical validated data
     */
    public void fetchHistoricalMeterReadings(DkEnerginetCustomerPermissionRequest permissionRequest) {
        ZonedDateTime end = permissionRequest.end();
        if (end != null && end.isBefore(ZonedDateTime.now(ZoneOffset.UTC))) {
            sink.tryEmitNext(permissionRequest);
        }
    }

    private Mono<IdentifiableApiResponse> fetch(DkEnerginetCustomerPermissionRequest permissionRequest) {
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);
        ZonedDateTime now = ZonedDateTime.now(DK_ZONE_ID);
        return permissionRequest.accessToken()
                // If we get an 401 Unauthorized error, the refresh token was revoked and the permission request with that
                .doOnError(error -> revokePermissionRequest(permissionRequest, error))
                .flatMap(accessToken -> energinetCustomerApi.getTimeSeries(
                        permissionRequest.lastPolled(),
                        now.isBefore(permissionRequest.end()) ? now : permissionRequest.end(),
                        permissionRequest.granularity(),
                        meteringPointsRequest,
                        accessToken,
                        UUID.fromString(permissionRequest.permissionId())
                ))
                .mapNotNull(MyEnergyDataMarketDocumentResponseListApiResponse::getResult)
                .map(response -> new IdentifiableApiResponse(permissionRequest.permissionId(),
                        permissionRequest.connectionId(), permissionRequest.dataNeedId(), response)
                )
                .doOnError(error -> LOGGER.error("Something went wrong while fetching data from Energinet:", error))
                .doOnSuccess(ignored -> permissionRequest.updateLastPolled(now));
    }

    private void revokePermissionRequest(DkEnerginetCustomerPermissionRequest permissionRequest,
                                         Throwable error) {
        if (!(error instanceof HttpClientErrorException.Unauthorized)) {
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
