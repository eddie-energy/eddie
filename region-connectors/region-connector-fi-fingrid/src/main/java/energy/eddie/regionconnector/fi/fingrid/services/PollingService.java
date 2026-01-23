// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fi.fingrid.services;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fi.fingrid.client.FingridApiClient;
import energy.eddie.regionconnector.fi.fingrid.client.model.CustomerDataResponse;
import energy.eddie.regionconnector.fi.fingrid.client.model.TimeSeriesResponse;
import energy.eddie.regionconnector.fi.fingrid.permission.events.SimpleEvent;
import energy.eddie.regionconnector.fi.fingrid.permission.request.FingridPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import energy.eddie.regionconnector.shared.utils.DateTimeUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class PollingService implements CommonPollingService<FingridPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final EnergyDataService energyDataService;
    private final FingridApiClient api;
    private final UpdateGranularityService updateGranularityService;
    private final DataNeedsService dataNeedsService;
    private final Outbox outbox;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // DataNeedsService is injected from parent context
    public PollingService(
            EnergyDataService energyDataService,
            FingridApiClient api,
            UpdateGranularityService updateGranularityService,
            DataNeedsService dataNeedsService,
            Outbox outbox
    ) {
        this.energyDataService = energyDataService;
        this.api = api;
        this.updateGranularityService = updateGranularityService;
        this.dataNeedsService = dataNeedsService;
        this.outbox = outbox;
    }

    @Override
    public void pollTimeSeriesData(FingridPermissionRequest permissionRequest) {
        pollTimeSeriesData(permissionRequest, permissionRequest.granularity());
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(FingridPermissionRequest permissionRequest) {
        if (isInactive(permissionRequest)) return false;
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = dataNeedsService.getById(dataNeedId);
        return dataNeed instanceof ValidatedHistoricalDataDataNeed;
    }

    public void pollTimeSeriesData(FingridPermissionRequest permissionRequest, Granularity granularity) {
        if (isInactive(permissionRequest)) {
            return;
        }
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        var start = permissionRequest.start().atStartOfDay(ZoneOffset.UTC);
        var now = LocalDate.now(ZoneOffset.UTC);
        var yesterday = now.minusDays(1);
        var end = permissionRequest.end().isAfter(yesterday) ? yesterday : permissionRequest.end();
        getKnownOrRequestMeterEANs(permissionRequest)
                .flatMap(meteringPointEAN ->
                                 api.getTimeSeriesData(
                                         meteringPointEAN,
                                         permissionRequest.customerIdentification(),
                                         permissionRequest.latestMeterReading(meteringPointEAN).orElse(start),
                                         DateTimeUtils.endOfDay(end, ZoneOffset.UTC),
                                         granularity.name(),
                                         null
                                 )
                )
                .collectList()
                .flatMap(resp -> updateGranularityService.updateGranularity(resp, permissionRequest))
                .subscribe(
                        energyDataService.publish(permissionRequest),
                        error -> handleError(permissionRequest, error)
                );
    }

    public Mono<List<TimeSeriesResponse>> forcePoll(
            FingridPermissionRequest permissionRequest,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        return getKnownOrRequestMeterEANs(permissionRequest)
                .flatMap(meteringPointEAN ->
                                 api.getTimeSeriesData(
                                         meteringPointEAN,
                                         permissionRequest.customerIdentification(),
                                         start,
                                         end,
                                         permissionRequest.granularity().name(),
                                         null
                                 )
                )
                .collectList()
                .doOnSuccess(res -> energyDataService.publishWithoutUpdating(res, permissionRequest))
                .doOnError(error -> handleError(permissionRequest, error));
    }

    public void pollAccountingPointData(FingridPermissionRequest permissionRequest) {
        LOGGER.atInfo()
              .addArgument(permissionRequest::permissionId)
              .log("Polling accounting point data for permission request {}");
        api.getCustomerData(permissionRequest.customerIdentification())
           .filter(res -> res.customerData().transaction() != null)
           .switchIfEmpty(Mono.fromCallable(() -> onEmptyCustomerData(permissionRequest)))
           .subscribe(
                   data -> energyDataService.publish(data, permissionRequest),
                   error -> handleError(permissionRequest, error)
           );
    }

    @Nullable
    private CustomerDataResponse onEmptyCustomerData(FingridPermissionRequest permissionRequest) {
        outbox.commit(new SimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.UNFULFILLABLE));
        return null;
    }

    private Flux<String> getKnownOrRequestMeterEANs(FingridPermissionRequest permissionRequest) {
        var meters = permissionRequest.meterEANs();
        return meters.isEmpty()
                ? api.getCustomerData(permissionRequest.customerIdentification())
                     .doOnSuccess(ignored -> LOGGER.atInfo()
                                                   .addArgument(permissionRequest::permissionId)
                                                   .log("Found new metering points for permission request {}"))
                     .flatMapIterable(res -> res.customerData().transaction().meteringPointEANs())
                : Flux.fromIterable(meters);
    }

    private static boolean isInactive(FingridPermissionRequest permissionRequest) {
        var now = LocalDate.now(ZoneOffset.UTC);
        return !permissionRequest.start().isBefore(now);
    }

    private void handleError(FingridPermissionRequest permissionRequest, Throwable error) {
        var permissionId = permissionRequest.permissionId();
        LOGGER.info("Error while requesting data for permission request {}", permissionId, error);
        if (error instanceof WebClientResponseException.Forbidden || error instanceof WebClientResponseException.Unauthorized) {
            LOGGER.info("Revoking permission request {}", permissionId);
            outbox.commit(new SimpleEvent(permissionId, PermissionProcessStatus.REVOKED));
        }
    }
}
