package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointDetailsCustomerDto;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.filter.EnerginetResolution;
import energy.eddie.regionconnector.dk.energinet.filter.IdentifiableApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.filter.MeteringDetailsApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkInternalGranularityEvent;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkUnfulfillableEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.ApiCredentials;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import energy.eddie.regionconnector.shared.validation.GranularityChoice;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.dk.energinet.EnerginetRegionConnectorMetadata.DK_ZONE_ID;

@Service
public class PollingService implements AutoCloseable, CommonPollingService<DkEnerginetPermissionRequest> {
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
            .filter(error -> error instanceof WebClientResponseException.TooManyRequests || error instanceof WebClientResponseException.ServiceUnavailable);
    public static final int REQUESTED_AGGREGATION_UNAVAILABLE = 30008; // from Eloverblik API documentation
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    public final IdentifiableApiResponseFilter identifiableApiResponseFilter = new IdentifiableApiResponseFilter();
    public final MeteringDetailsApiResponseFilter meteringDetailsApiResponseFilter = new MeteringDetailsApiResponseFilter();
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Flux<IdentifiableApiResponse> apiResponseFlux;
    private final Sinks.Many<IdentifiableApiResponse> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService;
    private final Outbox outbox;
    private final ObjectMapper objectMapper;
    private final DataNeedsService dataNeedsService;
    private final ApiExceptionService apiExceptionService;


    public PollingService(
            EnerginetCustomerApi energinetCustomerApi,
            MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService,
            Outbox outbox,
            ObjectMapper objectMapper,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
            DataNeedsService dataNeedsService,
            ApiExceptionService apiExceptionService
    ) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.meterReadingPermissionUpdateAndFulfillmentService = meterReadingPermissionUpdateAndFulfillmentService;
        this.objectMapper = objectMapper;
        this.dataNeedsService = dataNeedsService;
        this.apiExceptionService = apiExceptionService;
        apiResponseFlux = sink.asFlux()
                .share();
        this.outbox = outbox;
    }
    

    private boolean isActiveAndNeedsToBePolled(
            DkEnerginetPermissionRequest permissionRequest,
            LocalDate today
    ) {
        var dataNeed = dataNeedsService.findById(permissionRequest.dataNeedId());
        if (dataNeed.isEmpty() || !(dataNeed.get() instanceof ValidatedHistoricalDataDataNeed)) {
            return false;
        }
        var permissionStart = permissionRequest.start();
        return permissionStart.isBefore(today)
                && permissionRequest.latestMeterReadingEndDate()
                .map(lastPolled -> lastPolled.isBefore(today) || lastPolled.isEqual(today))
                .orElse(true);
    }

    private void fetch(DkEnerginetPermissionRequest permissionRequest, LocalDate today) {

        var meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        var meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);


        var dateFrom = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        var dateTo = Optional.of(permissionRequest.end())
                .filter(d -> d.isBefore(today))
                // The Energinet API is inclusive on the start date and exclusive on the end date,
                // so we need to add one day if the end date is before today
                .map(d -> d.plusDays(1))
                .orElse(today);
        var permissionId = permissionRequest.permissionId();

        LOGGER.info("Fetching metering data from Energinet for permission request {} from {} to {}",
                permissionId,
                dateFrom,
                dateTo);
        new ApiCredentials(
                energinetCustomerApi,
                permissionRequest.refreshToken(),
                permissionRequest.accessToken(),
                objectMapper
        ).accessToken()
                .flatMap(token -> tokenAndGranularity(permissionRequest, token, meteringPointsRequest))
                .flatMap(pair -> energinetCustomerApi.getTimeSeries(
                        dateFrom,
                        dateTo,
                        pair.granularity,
                        meteringPointsRequest,
                        pair.token,
                        UUID.fromString(permissionId)
                ))
                .retryWhen(RETRY_BACKOFF_SPEC)
                .mapNotNull(MyEnergyDataMarketDocumentResponseListApiResponse::getResult)
                .flatMap(myEnergyDataMarketDocumentResponses -> identifiableApiResponseFilter.filter(
                        permissionRequest,
                        dateFrom,
                        dateTo,
                        myEnergyDataMarketDocumentResponses))
                .doOnError(error -> apiExceptionService.handleError(permissionRequest.permissionId(), error))
                .onErrorComplete()
                .subscribe(identifiableApiResponse -> handleIdentifiableApiResponse(
                        permissionRequest,
                        identifiableApiResponse,
                        permissionId,
                        dateFrom,
                        dateTo
                ));
    }

    /**
     * Returns the token and granularity for the given permission request. If the granularity is not set, the
     * granularity will be fetched from the api and validated for compliance with the data need.
     */
    private Mono<TokenGranularityPair> tokenAndGranularity(
            DkEnerginetPermissionRequest permissionRequest,
            String token,
            MeteringPointsRequest meteringPointsRequest
    ) {
        if (permissionRequest.granularity() == null) {
            return fetchMeteringPointGranularity(permissionRequest, token, meteringPointsRequest);
        }
        return Mono.just(new TokenGranularityPair(token, permissionRequest.granularity()));
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

    private Mono<TokenGranularityPair> fetchMeteringPointGranularity(
            DkEnerginetPermissionRequest permissionRequest,
            String token,
            MeteringPointsRequest meteringPointsRequest
    ) {
        return energinetCustomerApi
                .getMeteringPointDetails(meteringPointsRequest, token)
                .flatMap(response -> meteringDetailsApiResponseFilter
                        .filter(permissionRequest.meteringPoint(), response))
                .flatMap(meteringPointDetailsCustomerDto -> handleMeteringPointDetails(permissionRequest,
                        token,
                        meteringPointDetailsCustomerDto));
    }

    private Mono<TokenGranularityPair> handleMeteringPointDetails(
            DkEnerginetPermissionRequest permissionRequest,
            String token,
            MeteringPointDetailsCustomerDto meteringPointDetailsCustomerDto
    ) {
        var resolution = meteringPointDetailsCustomerDto.getMeterReadingOccurrence();
        var granularity = validateResolutionAndMapToGranularity(
                permissionRequest,
                meteringPointDetailsCustomerDto.getMeterReadingOccurrence()
        );

        if (granularity.isEmpty()) {
            LOGGER.atWarn()
                    .addArgument(permissionRequest::permissionId)
                    .log("The metering point for permission request {} can not provide the data with the requested granularity.");
            String reason = "Metering point provides data with " + resolution + " granularity which is not between the min and max granularity of the data need";
            outbox.commit(new DkUnfulfillableEvent(permissionRequest.permissionId(), reason));
            return Mono.empty();
        }

        outbox.commit(new DkInternalGranularityEvent(permissionRequest.permissionId(), granularity.get()));
        return Mono.just(new TokenGranularityPair(token, granularity.get()));
    }

    private Optional<Granularity> validateResolutionAndMapToGranularity(
            DkEnerginetPermissionRequest permissionRequest,
            String resolution
    ) {
        var iso8601Duration = new EnerginetResolution(resolution).toISO8601Duration();
        Granularity granularity;
        try {
            granularity = Granularity.valueOf(iso8601Duration);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Could not parse resolution {} to granularity", resolution);
            return Optional.empty();
        }

        return dataNeedsService
                .findById(permissionRequest.dataNeedId())
                .map(dataNeed -> viableGranularity(dataNeed, granularity));
    }

    private @Nullable Granularity viableGranularity(DataNeed dataNeed, Granularity granularity) {
        if (dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed) {
            return switch (vhdDataNeed.minGranularity()) {
                // regardless of the resolution, these granularities are always available
                case P1D, P1Y, P1M -> vhdDataNeed.minGranularity();
                default -> GranularityChoice.isBetween(granularity,
                        vhdDataNeed.minGranularity(),
                        vhdDataNeed.maxGranularity())
                        ? granularity : null;
            };
        }
        return null;
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
        if (end.isBefore(now) || end.isEqual(now)) {
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

    @Override
    public void pollTimeSeriesData(DkEnerginetPermissionRequest activePermission) {
        var today = LocalDate.now(DK_ZONE_ID);
        fetch(activePermission, today);
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(DkEnerginetPermissionRequest permissionRequest) {
        var today = LocalDate.now(DK_ZONE_ID);
        return isActiveAndNeedsToBePolled(permissionRequest, today);
    }

    private record TokenGranularityPair(String token, Granularity granularity) {
    }
}
