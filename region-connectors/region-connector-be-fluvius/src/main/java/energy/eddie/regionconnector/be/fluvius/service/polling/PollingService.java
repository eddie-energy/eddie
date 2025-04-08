package energy.eddie.regionconnector.be.fluvius.service.polling;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.client.DataServiceType;
import energy.eddie.regionconnector.be.fluvius.client.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.client.model.GetEnergyResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.events.SimpleEvent;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.be.fluvius.streams.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(PollingService::isRetryable);
    private final BePermissionRequestRepository permissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final FluviusApiClient apiClient;
    private final IdentifiableDataStreams identifiableDataStreams;
    private final Outbox outbox;

    public PollingService(
            BePermissionRequestRepository permissionRequestRepository,
            DataNeedCalculationService<DataNeed> calculationService,
            FluviusApiClient apiClient,
            IdentifiableDataStreams identifiableDataStreams,
            Outbox outbox
    ) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.calculationService = calculationService;
        this.apiClient = apiClient;
        this.identifiableDataStreams = identifiableDataStreams;
        this.outbox = outbox;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public void poll(String permissionId) {
        LOGGER.info("Polling for permission request {}", permissionId);
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        if (permissionRequest.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            LOGGER.info("Permission request {} has not started yet", permissionId);
            return;
        }
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeedCalcResult = calculationService.calculate(dataNeedId, permissionRequest.created());
        switch (dataNeedCalcResult) {
            case ValidatedHistoricalDataDataNeedResult ignored -> pollValidatedHistoricalData(permissionRequest);
            default -> LOGGER.warn("DataNeed {} not supported for permission request {}", dataNeedId, permissionId);
        }
    }

    /**
     * Retries when the error is: - a 429 TooManyRequests - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        var retryable = e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
        LOGGER.info("Checking if error is retryable({})", retryable, e);
        return retryable;
    }

    private void pollValidatedHistoricalData(FluviusPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        for (var meter : permissionRequest.lastMeterReadings()) {
            var partitions = new RequestPartitions(permissionRequest, meter);
            for (var partition : partitions.partitions()) {
                var energyDataStart = partition.start();
                var energyDataEnd = partition.end();
                var energyDataType = DataServiceType.from(permissionRequest);
                LOGGER.info("Requesting validated historical data from {} to {} for permission request {}",
                            energyDataStart,
                            energyDataEnd,
                            permissionId);
                apiClient.energy(permissionId,
                                 meter.meterEan(),
                                 energyDataType,
                                 energyDataStart,
                                 energyDataEnd)
                         .retryWhen(RETRY_BACKOFF_SPEC)
                         .doOnSuccess(
                                 data -> LOGGER.debug("Got response from fluvius for permission request {}",
                                                      permissionId)
                         )
                         .filter(response -> isDataPresent(response, permissionId))
                         .subscribe(
                                 res -> identifiableDataStreams.publish(permissionRequest, res),
                                 error -> handleFetchError(permissionId, energyDataStart, energyDataEnd, error)
                         );
            }
        }
    }

    private boolean isDataPresent(GetEnergyResponseModelApiDataResponse response, String permissionId) {
        if (response.getData() != null && response.getData().getElectricityMeters() != null) {
            return true;
        }
        LOGGER.info("Response for energy data was empty, for permission request {}", permissionId);
        return false;
    }

    private void handleFetchError(String permissionId, ZonedDateTime start, ZonedDateTime end, Throwable e) {
        LOGGER.error("Error while fetching data from Fluvius for permissionId '{}' from '{}' to '{}'",
                     permissionId, start, end, e);
        if (e instanceof WebClientResponseException.Forbidden || e instanceof WebClientResponseException.Unauthorized) {
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        }
    }
}
