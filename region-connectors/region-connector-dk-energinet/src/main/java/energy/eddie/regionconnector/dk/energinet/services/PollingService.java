package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.filter.IdentifiableApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.ApiCredentials;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.persistence.DkPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
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
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

@Service
public class PollingService implements AutoCloseable {
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(error -> error instanceof WebClientResponseException.TooManyRequests || error instanceof WebClientResponseException.ServiceUnavailable);
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Flux<IdentifiableApiResponse> apiResponseFlux;
    private final Sinks.Many<IdentifiableApiResponse> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final DkPermissionRequestRepository repository;
    private final MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService;
    private final Outbox outbox;
    private final ObjectMapper objectMapper;


    public PollingService(
            EnerginetCustomerApi energinetCustomerApi,
            DkPermissionRequestRepository repository,
            MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService,
            Outbox outbox,
            ObjectMapper objectMapper
    ) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.repository = repository;
        this.meterReadingPermissionUpdateAndFulfillmentService = meterReadingPermissionUpdateAndFulfillmentService;
        this.objectMapper = objectMapper;
        apiResponseFlux = sink.asFlux()
                              .share();
        this.outbox = outbox;
    }

    /**
     * Fetches future meter readings for all accepted active permission requests.
     */
    @SuppressWarnings("java:S6857") // Sonar thinks this is malformed, but it's not
    @Scheduled(cron = "${region-connector.dk.energinet.polling:0 0 17 * * *}", zone = "Europe/Copenhagen")
    public void fetchFutureMeterReadings() {
        var acceptedPermissionRequests = repository.findAllByStatus(PermissionProcessStatus.ACCEPTED);
        LOGGER.info("Trying to fetch meter readings for {} permission requests", acceptedPermissionRequests.size());
        LocalDate today = LocalDate.now(DK_ZONE_ID);

        for (DkEnerginetPermissionRequest acceptedPermissionRequest : acceptedPermissionRequests) {
            if (isActiveAndNeedsToBePolled(acceptedPermissionRequest, today)) {
                fetch(acceptedPermissionRequest, today);
            } else {
                var permissionId = acceptedPermissionRequest.permissionId();
                LOGGER.info("Permission request {} is not active or data is already up to date", permissionId);
            }
        }
    }

    private static boolean isActiveAndNeedsToBePolled(
            DkEnerginetPermissionRequest permissionRequest,
            LocalDate today
    ) {
        LocalDate permissionStart = permissionRequest.start();
        return permissionStart.isBefore(today)
                && permissionRequest.latestMeterReadingEndDate()
                                    .map(lastPolled -> lastPolled.isBefore(today) || lastPolled.isEqual(today))
                                    .orElse(true);
    }

    private void fetch(DkEnerginetPermissionRequest permissionRequest, LocalDate today) {
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);

        LocalDate dateFrom = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        LocalDate dateTo = Optional.of(permissionRequest.end())
                                   .filter(d -> d.isBefore(today))
                                   // The Energinet API is inclusive on the start date and exclusive on the end date,
                                   // so we need to add one day if the end date is before today
                                   .map(d -> d.plusDays(1))
                                   .orElse(today);
        String permissionId = permissionRequest.permissionId();

        LOGGER.info("Fetching metering data from Energinet for permission request {} from {} to {}",
                    permissionId,
                    dateFrom,
                    dateTo);
        new ApiCredentials(energinetCustomerApi,
                           permissionRequest.refreshToken(),
                           permissionRequest.accessToken(),
                           objectMapper)
                .accessToken()
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
                                 new IdentifiableApiResponseFilter(permissionRequest, dateFrom, dateTo)
                                         .filter(myEnergyDataMarketDocumentResponses))
                .doOnError(error -> LOGGER.error(
                        "Something went wrong while fetching data for permission request {} from Energinet:",
                        permissionId,
                        error))
                .onErrorComplete()
                .subscribe(identifiableApiResponse ->
                                   handleIdentifiableApiResponse(
                                           permissionRequest,
                                           identifiableApiResponse,
                                           permissionId,
                                           dateFrom,
                                           dateTo
                                   )
                );
    }

    private void revokePermissionRequest(
            DkEnerginetPermissionRequest permissionRequest,
            Throwable error
    ) {
        if (!(error instanceof WebClientResponseException.Unauthorized)) {
            LOGGER.warn("Got an unexpected error while requesting access token", error);
            return;
        }
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Revoking permission request with permission id {}", permissionId);
        outbox.commit(new DkSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
    }

    private void handleIdentifiableApiResponse(
            DkEnerginetPermissionRequest permissionRequest,
            IdentifiableApiResponse identifiableApiResponse,
            String permissionId,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        LOGGER.info("Fetched metering data from Energinet for permission request {} from {} to {}",
                    permissionId,
                    dateFrom,
                    dateTo);
        meterReadingPermissionUpdateAndFulfillmentService.tryUpdateAndFulfillPermissionRequest(
                permissionRequest,
                identifiableApiResponse
        );
        sink.emitNext(identifiableApiResponse,
                      Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    /**
     * This will try to fetch historical meter readings for the given permission request. If the permission request is
     * for future data, no data will be fetched.
     *
     * @param permissionRequest for historical validated data
     */
    public void fetchHistoricalMeterReadings(DkEnerginetPermissionRequest permissionRequest) {
        LocalDate end = permissionRequest.end();
        LocalDate now = LocalDate.now(DK_ZONE_ID);
        if (end != null && (end.isBefore(now) || end.isEqual(now))) {
            fetch(permissionRequest, now);
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
