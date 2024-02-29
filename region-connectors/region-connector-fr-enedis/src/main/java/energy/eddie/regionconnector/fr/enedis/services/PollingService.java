package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.fr.enedis.api.EnedisApi;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.providers.IdentifiableMeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;

@Service
public class PollingService implements AutoCloseable {
    public static final int MAXIMUM_PERMISSION_DURATION = 3;
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1)).filter(PollingService::isRetryable);
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnedisApi enedisApi;
    private final Sinks.Many<IdentifiableMeterReading> meterReadings;

    public PollingService(EnedisApi enedisApi, Sinks.Many<IdentifiableMeterReading> meterReadings) {
        this.enedisApi = enedisApi;
        this.meterReadings = meterReadings;
    }

    /**
     * Retries when the error is:
     * - a 429 TooManyRequests
     * - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        return e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
    }

    private static void handleError(FrEnedisPermissionRequest permissionRequest, LocalDate start, String permissionId, Throwable e, LocalDate finalEndOfDataRequest) {
        LOGGER.error("Error while fetching data from ENEDIS for permissionId '{}' from '{}' to '{}'", permissionId, start, finalEndOfDataRequest, e);
        if (e instanceof WebClientResponseException.Forbidden) {
            LOGGER.warn("Revoking permission request for permissionId '{}'", permissionId);
            try {
                permissionRequest.revoke();
            } catch (StateTransitionException ex) {
                LOGGER.warn("Unable to revoke permission request", ex);
            }
        }
    }

    @Async
    public void fetchHistoricalMeterReadings(FrEnedisPermissionRequest permissionRequest) {
        LocalDate permissionStart = permissionRequest.start().toLocalDate();
        LocalDate permissionEnd = Optional.ofNullable(permissionRequest.end())
                .map(ZonedDateTime::toLocalDate)
                .orElse(permissionStart.plusYears(MAXIMUM_PERMISSION_DURATION));

        LocalDate now = LocalDate.now(ZONE_ID_FR);
        String permissionId = permissionRequest.permissionId();
        if (!permissionStart.isBefore(now)) {
            LOGGER.info("Permission request '{}' is not yet active, skipping data fetch", permissionId);
            return;
        }

        var end = now.isAfter(permissionEnd) ? permissionEnd.plusDays(1) : now;
        LOGGER.info("Preparing to fetch data from ENEDIS for permission request '{}' from '{}' to '{}' (inclusive)", permissionId, permissionStart, end);

        // If the granularity is PT30M, we need to fetch the data in batches
        switch (permissionRequest.granularity()) {
            case PT30M -> fetchDataInBatches(permissionRequest, permissionStart, end, permissionId)
                    .subscribe(meterReadings::tryEmitNext);
            case P1D -> fetchData(permissionRequest, end, permissionId, permissionStart)
                    .subscribe(meterReadings::tryEmitNext);
            default -> throw new IllegalStateException("Unsupported granularity: " + permissionRequest.granularity());
        }
    }

    private Flux<IdentifiableMeterReading> fetchDataInBatches(FrEnedisPermissionRequest permissionRequest, LocalDate permissionStart, LocalDate end, String permissionId) {
        return calculateBatchDates(permissionStart, end)
                .flatMap(batchStart -> {
                    // Calculate the end date for this batch, ensuring it's within the overall end date and not in the future
                    LocalDate batchEnd = batchStart.plusWeeks(1);
                    batchEnd = batchEnd.isAfter(end) ? end : batchEnd; // Ensure not to exceed the end date
                    return fetchData(permissionRequest, batchEnd, permissionId, batchStart);
                })
                .onErrorComplete(); // stop the stream if an error occurs
    }

    private Mono<IdentifiableMeterReading> fetchData(FrEnedisPermissionRequest permissionRequest, LocalDate end, String permissionId, LocalDate batchStart) {
        LOGGER.info("Fetching data from ENEDIS for permissionId '{}' from '{}' to '{}'", permissionId, batchStart, end);
        // Make the API call for this batch
        return Mono.defer(() -> enedisApi.getConsumptionMeterReading(permissionRequest.usagePointId().orElseThrow(), batchStart, end, permissionRequest.granularity()))
                .retryWhen(RETRY_BACKOFF_SPEC)
                .doOnError(e -> handleError(permissionRequest, batchStart, permissionId, e, end))
                .map(meterReading -> new IdentifiableMeterReading(permissionRequest, meterReading));
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