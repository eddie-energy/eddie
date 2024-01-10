package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.ConsumptionRecord;
import energy.eddie.api.v0.Mvp1ConsumptionRecordProvider;
import energy.eddie.regionconnector.es.datadis.ConsumptionRecordMapper;
import energy.eddie.regionconnector.es.datadis.InvalidMappingException;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.api.UnauthorizedException;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.dtos.Supply;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.InvalidPointAndMeasurementTypeCombinationException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSuppliesException;
import energy.eddie.regionconnector.es.datadis.dtos.exceptions.NoSupplyForMeteringPointException;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.function.Predicate;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

@Component
public class DatadisScheduler implements Mvp1ConsumptionRecordProvider, AutoCloseable {
    private final DataApi dataApi;
    private final Sinks.Many<ConsumptionRecord> consumptionRecords;

    @Autowired
    public DatadisScheduler(DataApi dataApi, Sinks.Many<ConsumptionRecord> consumptionRecords) {
        this.dataApi = dataApi;
        this.consumptionRecords = consumptionRecords;
    }

    private static Predicate<MeteringData> notInRequestedRange(LocalDate from, LocalDate to) {
        return m -> m.date() == null || m.date().isBefore(from) || m.date().isAfter(to);
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
                .retryWhen(Retry.fixedDelay(10, Duration.ofMinutes(1))) // after the user has accepted the permission, the data might not be available immediately
                .flatMap(supplies -> prepareMeteringDataRequest(permissionRequest, supplies))
                .flatMap(dataApi::getConsumptionKwh)
                .flatMap(meteringData -> processMeteringData(
                        meteringData, permissionRequest, consumptionRecords))
                .doOnError(e -> {
                    Throwable cause = e;
                    while (cause.getCause() != null) { // do match the exception we need to get the cause
                        cause = cause.getCause();
                    }
                    if (cause instanceof NoSuppliesException) {
                        // this could mean that the user has no metering point associated with his account,
                        // or that the supplier responsible for the account is currently not reachable
                        // we should think of a new state for this
                        // calling changeState is also bad here, because it will not be persisted or propagated
                        // fix with #296
                    } else if (cause instanceof InvalidPointAndMeasurementTypeCombinationException) {
                        // we should think of a new state for this
                        // fix with #296
                    } else if (cause instanceof UnauthorizedException) {
                        // The authorization has not actually been granted or was revoked change to revoked state
                        // fix with #296
                    }
                    // In the case of a NoSupplyForMeteringPointException, we can assume that the distributor is
                    // currently not reachable and we should retry later
                })
                .subscribe();
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
        LocalDate from = permissionRequest.requestDataFrom().toLocalDate();
        // check if request data from is in the future or today
        if (from.isAfter(now) || from.isEqual(now)) {
            return Mono.empty(); // the start of the permission is in the future, so we can't pull any metering data yet
        }

        LocalDate to = permissionRequest.requestDataTo().toLocalDate().isAfter(now)
                ? now.minusDays(1)
                : permissionRequest.requestDataTo().toLocalDate();

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

    private Mono<Void> processMeteringData(
            List<MeteringData> meteringData,
            EsPermissionRequest permissionRequest,
            Sinks.Many<ConsumptionRecord> consumptionRecordSink) {

        var from = permissionRequest.requestDataFrom().toLocalDate();
        var to = permissionRequest.requestDataTo().toLocalDate();

        // remove metering data that is not in the requested time range
        meteringData.removeIf(notInRequestedRange(from, to));
        permissionRequest.setLastPulledMeterReading(Objects.requireNonNull(meteringData.get(meteringData.size() - 1).date()).atStartOfDay(ZoneOffset.UTC));

        try {
            ConsumptionRecord consumptionRecord = ConsumptionRecordMapper.mapToCIM(
                    meteringData,
                    permissionRequest.permissionId(),
                    permissionRequest.connectionId(),
                    permissionRequest.measurementType(),
                    permissionRequest.dataNeedId()
            );
            consumptionRecordSink.tryEmitNext(consumptionRecord);
            return Mono.empty();
        } catch (InvalidMappingException e) {
            consumptionRecordSink.tryEmitError(e);
            return Mono.error(e);
        }
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
    public Flow.Publisher<ConsumptionRecord> getConsumptionRecordStream() {
        return JdkFlowAdapter.publisherToFlowPublisher(consumptionRecords.asFlux());
    }

    @Override
    public void close() {
        consumptionRecords.tryEmitComplete();
    }
}