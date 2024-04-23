package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.events.FrSimpleEvent;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
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
import java.time.temporal.ChronoUnit;

@Service
public class PollingService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(PollingService::isRetryable);
    private final EnedisApi enedisApi;
    private final Sinks.Many<IdentifiableMeterReading> meterReadings;
    private final MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService;
    private final Outbox outbox;

    public PollingService(
            EnedisApi enedisApi,
            MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService,
            Sinks.Many<IdentifiableMeterReading> meterReadings, Outbox outbox
    ) {
        this.enedisApi = enedisApi;
        this.meterReadings = meterReadings;
        this.meterReadingPermissionUpdateAndFulfillmentService = meterReadingPermissionUpdateAndFulfillmentService;
        this.outbox = outbox;
    }

    /**
     * Retries when the error is: - a 429 TooManyRequests - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        var retryable = e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
        LOGGER.info("Checking if error is retryable({})", retryable, e);
        return retryable;
    }

    public void fetchMeterReadings(
            FrEnedisPermissionRequest permissionRequest,
            LocalDate start,
            LocalDate end,
            String usagePointId
    ) {
        String permissionId = permissionRequest.permissionId();
        LOGGER.info("Preparing to fetch data from ENEDIS for permission request '{}' from '{}' to '{}' (inclusive)",
                    permissionId, start, end);

        // If the granularity is PT30M, we need to fetch the data in batches
        switch (permissionRequest.granularity()) {
            case PT30M -> fetchDataInBatches(permissionRequest, start, end, usagePointId)
                    .doOnComplete(() -> LOGGER.info(
                            "Finished fetching half hourly data from ENEDIS for permission request '{}'",
                            permissionId))
                    .subscribe(identifiableMeterReading -> handleIdentifiableMeterReading(
                                       Granularity.PT30M,
                                       permissionRequest,
                                       identifiableMeterReading
                               )
                    );
            case P1D -> fetchData(permissionRequest, start, end, usagePointId)
                    .subscribe(identifiableMeterReading -> handleIdentifiableMeterReading(
                                       Granularity.P1D,
                                       permissionRequest,
                                       identifiableMeterReading
                               )
                    );
            default -> throw new IllegalStateException("Unsupported granularity: " + permissionRequest.granularity());
        }
    }

    private void handleIdentifiableMeterReading(
            Granularity granularity,
            FrEnedisPermissionRequest permissionRequest,
            IdentifiableMeterReading identifiableMeterReading
    ) {
        LOGGER.atInfo()
              .addArgument(granularity)
              .addArgument(permissionRequest::permissionId)
              .log("Fetched data with {} granularity from ENEDIS for permission request '{}'");

        meterReadingPermissionUpdateAndFulfillmentService.tryUpdateAndFulfillPermissionRequest(
                permissionRequest,
                identifiableMeterReading
        );
        meterReadings.emitNext(identifiableMeterReading,
                               Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    private Mono<IdentifiableMeterReading> fetchData(
            FrEnedisPermissionRequest permissionRequest,
            LocalDate start,
            LocalDate end,
            String usagePointId
    ) {
        String permissionId = permissionRequest.permissionId();
        LOGGER.info("Fetching data from ENEDIS for permissionId '{}' from '{}' to '{}'", permissionId, start, end);
        // Make the API call for this batch
        return Mono.defer(() -> enedisApi.getConsumptionMeterReading(permissionRequest.usagePointId()
                                                                                      .orElse(usagePointId),
                                                                     start,
                                                                     end,
                                                                     permissionRequest.granularity()))
                   .retryWhen(RETRY_BACKOFF_SPEC)
                   .doOnError(e -> handleError(permissionId, start, end, e))
                   .map(meterReading -> new IdentifiableMeterReading(permissionRequest, meterReading));
    }

    private void handleError(String permissionId, LocalDate start, LocalDate end, Throwable e) {
        LOGGER.error("Error while fetching data from ENEDIS for permissionId '{}' from '{}' to '{}'",
                     permissionId, start, end, e);
        if (e instanceof WebClientResponseException.Forbidden) {
            LOGGER.warn("Revoking permission request for permissionId '{}'", permissionId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        }
    }


    private Flux<IdentifiableMeterReading> fetchDataInBatches(
            FrEnedisPermissionRequest permissionRequest,
            LocalDate start,
            LocalDate end,
            String usagePointId
    ) {
        return calculateBatchDates(start, end)
                .flatMap(batchStart -> {
                    // Calculate the end date for this batch, ensuring it's within the overall end date and not in the future
                    LocalDate batchEnd = batchStart.plusWeeks(1);
                    batchEnd = batchEnd.isAfter(end) ? end : batchEnd; // Ensure not to exceed the end date
                    return fetchData(permissionRequest, batchStart, batchEnd, usagePointId);
                })
                .onErrorComplete(); // stop the stream if an error occurs
    }

    private Flux<LocalDate> calculateBatchDates(LocalDate start, LocalDate end) {
        // the api allows for a maximum of 7 days per request, so we need to split the request into multiple batches
        long daysBetween = ChronoUnit.DAYS.between(start, end);
        return Flux.range(0, (int) Math.ceil(daysBetween / 7.0))
                   .map(start::plusWeeks)
                   .takeWhile(batchStart -> !batchStart.isAfter(end)); // Ensure not to exceed the end date
    }

    @Override
    public void close() throws Exception {
        meterReadings.tryEmitComplete();
    }
}
