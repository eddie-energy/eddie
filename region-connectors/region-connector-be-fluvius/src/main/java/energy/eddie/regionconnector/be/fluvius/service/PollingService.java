package energy.eddie.regionconnector.be.fluvius.service;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApi;
import energy.eddie.regionconnector.be.fluvius.clients.FluviusApiClient;
import energy.eddie.regionconnector.be.fluvius.dtos.IdentifiableMeteringData;
import energy.eddie.regionconnector.be.fluvius.permission.request.FluviusPermissionRequest;
import energy.eddie.regionconnector.be.fluvius.persistence.BePermissionRequestRepository;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final BePermissionRequestRepository permissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final FluviusApiClient apiClient;
    private final Sinks.Many<IdentifiableMeteringData> meteringData;
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
            .filter(PollingService::isRetryable);

    public PollingService(BePermissionRequestRepository permissionRequestRepository, DataNeedCalculationService<DataNeed> calculationService, FluviusApiClient apiClient, Sinks.Many<IdentifiableMeteringData> meteringData) {
        this.permissionRequestRepository = permissionRequestRepository;
        this.calculationService = calculationService;
        this.apiClient = apiClient;
        this.meteringData = meteringData;
    }

    /**
     * Retries when the error is: - a 429 TooManyRequests - a 401 Unauthorized (i.e. when the token is expired)
     */
    private static boolean isRetryable(Throwable e) {
        var retryable = e instanceof WebClientResponseException.TooManyRequests || e instanceof WebClientResponseException.Unauthorized;
        LOGGER.info("Checking if error is retryable({})", retryable, e);
        return retryable;
    }

    public void poll(String permissionId) {
        LOGGER.info("Polling for permission request {}", permissionId);
        var permissionRequest = permissionRequestRepository.getByPermissionId(permissionId);
        if (permissionRequest.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            LOGGER.info("Permission request {} has not started yet", permissionId);
            return;
        }
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeedCalcResult = calculationService.calculate(dataNeedId);
        if (!(dataNeedCalcResult instanceof ValidatedHistoricalDataDataNeedResult vhdResult)) {
            LOGGER.warn("DataNeed {} not supported for permission request {}", dataNeedId, permissionId);
            return;
        }
        pollValidatedHistoricalData(permissionRequest, vhdResult);
    }

    private void pollValidatedHistoricalData(
            FluviusPermissionRequest permissionRequest,
            ValidatedHistoricalDataDataNeedResult vhdResult
    ) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);

        var energyDataStart = calcEnergyDataStart(permissionRequest, vhdResult);
        var energyDataEnd = calcEnergyDataEnd(vhdResult);

        // TODO: I think I saw some restriction, that we can only access data from 3 months ... handle this case!

        var energyDataType = switch (permissionRequest.granularity()) {
            case PT15M -> FluviusApi.DataServiceType.QUARTER_HOURLY;
            case P1D -> FluviusApi.DataServiceType.DAILY;
            default -> throw new IllegalStateException("Unexpected granularity: " + permissionRequest.granularity());
        };

        Flux
                .defer(() ->
                        apiClient.energy(
                                permissionId,
                                permissionRequest.eanNumber(), energyDataType,
                                energyDataStart,
                                energyDataEnd
                        )
                )
                .retryWhen(RETRY_BACKOFF_SPEC)
                .doOnError(e -> handleFetchError(permissionId, energyDataStart, energyDataEnd, e))
                .map(energyResponse -> new IdentifiableMeteringData(
                        permissionRequest,
                        energyResponse.getData() != null ? energyResponse.getData().getElectricityMeters() : List.of()
                ))
                .subscribe(identifiableMeteringData ->
                        meteringData.emitNext(
                                identifiableMeteringData,
                                Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1))
                        )
                );
    }

    private void handleFetchError(String permissionId, ZonedDateTime start, ZonedDateTime end, Throwable e) {
        LOGGER.error("Error while fetching data from Fluvius for permissionId '{}' from '{}' to '{}'",
                permissionId, start, end, e);
    }

    private ZonedDateTime calcEnergyDataStart(FluviusPermissionRequest permissionRequest, ValidatedHistoricalDataDataNeedResult vhdResult) {
        var dataNeedStart = vhdResult.energyTimeframe().start().atStartOfDay(ZoneOffset.UTC);
        var permissionStart = ZonedDateTime.of(permissionRequest.start().atStartOfDay(), ZoneOffset.UTC);
        if (dataNeedStart.isBefore(permissionStart)) {
            return permissionStart;
        }
        return dataNeedStart;
    }

    private ZonedDateTime calcEnergyDataEnd(ValidatedHistoricalDataDataNeedResult vhdResult) {
        var end = DateTimeUtils.endOfDay(vhdResult.energyTimeframe().end(), ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC);
        if (end.isAfter(now)) {
            return end;
        }
        return now;
    }
}
