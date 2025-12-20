package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.be.fluvius.client.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.permission.request.MeterReading;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class PollingService implements CommonPollingService<FluviusPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(PollingService::isRetryable);
    private final FluviusApiClient apiClient;
    private final IdentifiableDataStreams identifiableDataStreams;
    private final Outbox outbox;

    public PollingService(
            FluviusApiClient apiClient,
            IdentifiableDataStreams identifiableDataStreams,
            Outbox outbox
    ) {
        this.apiClient = apiClient;
        this.identifiableDataStreams = identifiableDataStreams;
        this.outbox = outbox;
    }

    public Flux<GetEnergyResponseModelApiDataResponse> forcePoll(
            FluviusPermissionRequest permissionRequest,
            LocalDate from,
            LocalDate to
    ) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling for permission request {} from {} to {}", permissionId, from, to);
        Flux<GetEnergyResponseModelApiDataResponse> commonflux = Flux.empty();
        for (var meter : permissionRequest.lastMeterReadings()) {
            meter = new MeterReading(permissionId, meter.meterEan(), null);
            var partitions = new RequestPartitions(from, to, meter);
            commonflux = Flux.merge(commonflux, pollAllPartitions(permissionRequest, partitions, meter));
        }
        emitResult(permissionRequest, commonflux, permissionId);
        return commonflux;
    }

    @Override
    public void pollTimeSeriesData(FluviusPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        Flux<GetEnergyResponseModelApiDataResponse> commonflux = Flux.empty();
        for (var meter : permissionRequest.lastMeterReadings()) {
            var partitions = new RequestPartitions(permissionRequest, meter);
            commonflux = Flux.merge(commonflux, pollAllPartitions(permissionRequest, partitions, meter));
        }
        emitResult(permissionRequest, commonflux, permissionId);
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(FluviusPermissionRequest permissionRequest) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start()).isBefore(now);
    }

    /**
     * Retries when the error is: - a 429 TooManyRequests - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        var retryable = e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
        LOGGER.info("Checking if error is retryable({})", retryable, e);
        return retryable;
    }

    private Flux<GetEnergyResponseModelApiDataResponse> pollAllPartitions(
            FluviusPermissionRequest permissionRequest,
            RequestPartitions partitions,
            MeterReading meter
    ) {
        Flux<GetEnergyResponseModelApiDataResponse> commonflux = Flux.empty();
        var permissionId = permissionRequest.permissionId();
        for (var partition : partitions.partitions()) {
            var energyDataStart = partition.start();
            var energyDataEnd = partition.end();
            LOGGER.info("Requesting validated historical data from {} to {} for permission request {}",
                        energyDataStart,
                        energyDataEnd,
                        permissionId);
            var energyDataType = DataServiceType.from(permissionRequest);
            var response = apiClient.energy(permissionId,
                                            meter.meterEan(),
                                            energyDataType,
                                            energyDataStart,
                                            energyDataEnd)
                                    .retryWhen(RETRY_BACKOFF_SPEC)
                                    .doOnSuccess(data -> LOGGER.atDebug()
                                                               .addArgument(permissionId)
                                                               .log("Got response from fluvius for permission request {}"))
                                    .filter(response1 -> isDataPresent(response1, permissionId));
            commonflux = Flux.merge(commonflux, response);
        }
        return commonflux;
    }

    private boolean isDataPresent(GetEnergyResponseModelApiDataResponse response, String permissionId) {
        if (response.data().electricityMeters() != null) {
            return true;
        }
        LOGGER.info("Response for energy data was empty, for permission request {}", permissionId);
        return false;
    }

    private void emitResult(
            FluviusPermissionRequest permissionRequest,
            Flux<GetEnergyResponseModelApiDataResponse> commonflux,
            String permissionId
    ) {
        commonflux
                .subscribe(
                        res -> identifiableDataStreams.publish(permissionRequest, res),
                        error -> handleFetchError(permissionId, error)
                );
    }

    private void handleFetchError(String permissionId, Throwable e) {
        LOGGER.error("Error while fetching data from Fluvius for permissionId '{}'", permissionId, e);
        if (e instanceof WebClientResponseException.Forbidden || e instanceof WebClientResponseException.Unauthorized) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        }
    }
}
