package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

@Component
public class DatadisScheduler implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisScheduler.class);
    private static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry
            .backoff(10, Duration.ofMinutes(2))
            .maxBackoff(Duration.ofHours(2))
            .filter(ex -> !isUnauthorized(ex));
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> meteringDataSink;

    @Autowired
    public DatadisScheduler(DataApi dataApi, Sinks.Many<IdentifiableMeteringData> meteringDataSink) {
        this.dataApi = dataApi;
        this.meteringDataSink = meteringDataSink;
    }

    private static boolean isUnauthorized(Throwable ex) {
        for (var exception = ex; exception != null; exception = exception.getCause()) {
            if (exception instanceof DatadisApiException apiException) {
                return apiException.statusCode() == HttpStatus.UNAUTHORIZED.value();
            }
        }
        return false;
    }

    private static void onError(EsPermissionRequest permissionRequest, Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) { // do match the exception we need to get the cause
            cause = cause.getCause();
        }
        switch (cause) {
            case NoSuppliesException ignored:
                // this could mean that the user has no metering point associated with his account,
                // or that the supplier responsible for the account is currently not reachable
                // we should think of a new state for this
                // calling changeState is also bad here, because it will not be persisted or propagated
                // fix with #296
                break;
            case InvalidPointAndMeasurementTypeCombinationException ignored:
                // we should think of a new state for this
                // fix with #296
                break;
            case DatadisApiException exception:
                if (exception.statusCode() == HttpStatus.FORBIDDEN.value()) {
                    try {
                        permissionRequest.revoke();
                    } catch (StateTransitionException ex) {
                        LOGGER.warn("Error revoking permission request", ex);
                    }
                }
                break;
            default:
                break;
        }
    }

    // Suppress the warning of multiple identical paths, should be removed when implementing issue #296
    @SuppressWarnings("java:S1871")
    public void pullAvailableHistoricalData(EsPermissionRequest permissionRequest) {
        var now = ZonedDateTime.now(ZONE_ID_SPAIN).toLocalDate();
        if (permissionRequest.permissionStart().toLocalDate().isAfter(now)) {
            // the start of the permission is in the future, so we can't pull any data yet.
            return;
        }

        dataApi.getSupplies(permissionRequest.nif(), null)
                .flatMap(this::validateSupplies)
                .retryWhen(RETRY_BACKOFF_SPEC) // after the user has accepted the permission, the data might not be available immediately, since Datadis only starts validating the permission after the user has accepted it
                .flatMap(supplies -> prepareMeteringDataRequest(permissionRequest, supplies))
                .flatMap(dataApi::getConsumptionKwh)
                .map(result -> new IdentifiableMeteringData(permissionRequest, result))
                .doOnError(e -> onError(permissionRequest, e))
                .subscribe(meteringDataSink::tryEmitNext);
    }

    private Mono<List<Supply>> validateSupplies(List<Supply> supplies) {
        return supplies.isEmpty()
                ? Mono.error(new NoSuppliesException("No supplies found"))
                : Mono.just(supplies);
    }

    private Mono<MeteringDataRequest> prepareMeteringDataRequest(EsPermissionRequest permissionRequest, List<Supply> supplies) {
        var optionalSupply = findSupplyWithCorrectMeteringPoint(supplies, permissionRequest);

        if (optionalSupply.isEmpty()) {
            return Mono.error(new NoSupplyForMeteringPointException("No supply found for metering point " + permissionRequest.meteringPointId()));
        }

        Supply supply = optionalSupply.get();
        permissionRequest.setDistributorCodeAndPointType(DistributorCode.fromCode(supply.distributorCode()), supply.pointType());

        if (!pointTypeSupportsMeasurementType(supply.pointType(), permissionRequest.measurementType()))
            return Mono.error(new InvalidPointAndMeasurementTypeCombinationException(supply.pointType(), permissionRequest.measurementType()));

        LocalDate now = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate();
        LocalDate from = permissionRequest.start().toLocalDate();
        // check if request data from is in the future or today
        if (from.isAfter(now) || from.isEqual(now)) {
            return Mono.empty(); // the start of the permission is in the future, so we can't pull any metering data yet
        }

        LocalDate to = Objects.requireNonNull(permissionRequest.end()).toLocalDate().isAfter(now)
                ? now.minusDays(1)
                : permissionRequest.end().toLocalDate();

        return Mono.just(new MeteringDataRequest(
                permissionRequest.nif(),
                permissionRequest.meteringPointId(),
                supply.distributorCode(),
                from,
                to,
                permissionRequest.measurementType(),
                String.valueOf(supply.pointType())
        ));
    }

    private Optional<Supply> findSupplyWithCorrectMeteringPoint(List<Supply> supplies, EsPermissionRequest permissionRequest) {
        return supplies.stream()
                .filter(s -> Objects.equals(s.meteringPoint(), permissionRequest.meteringPointId()))
                .findFirst();
    }

    /**
     * Check if the point type supports the measurement type.
     * All point types support hourly data.
     * Only point types 1 and 2 support quarter hourly data.
     */
    private boolean pointTypeSupportsMeasurementType(Integer pointType, MeasurementType measurementType) {
        return measurementType == MeasurementType.HOURLY ||
                (pointType == 1 && measurementType == MeasurementType.QUARTER_HOURLY) ||
                (pointType == 2 && measurementType == MeasurementType.QUARTER_HOURLY);
    }

    @Override
    public void close() {
        meteringDataSink.tryEmitComplete();
    }
}