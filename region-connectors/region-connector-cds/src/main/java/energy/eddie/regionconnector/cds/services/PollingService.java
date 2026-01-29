// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.cds.client.CdsServerClientFactory;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientErrorHandler;
import energy.eddie.regionconnector.cds.openapi.model.*;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple5;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Service
@Transactional(Transactional.TxType.REQUIRED)
public class PollingService implements CommonPollingService<CdsPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final CdsServerClientFactory factory;
    private final IdentifiableDataStreams streams;
    private final CustomerDataClientErrorHandler errorHandler;

    public PollingService(
            DataNeedCalculationService<DataNeed> calculationService,
            CdsServerClientFactory factory,
            IdentifiableDataStreams streams,
            CustomerDataClientErrorHandler errorHandler
    ) {
        this.calculationService = calculationService;
        this.factory = factory;
        this.streams = streams;
        this.errorHandler = errorHandler;
    }

    public void poll(CdsPermissionRequest permissionRequest) {
        var permissionId = permissionRequest.permissionId();
        var dataNeedId = permissionRequest.dataNeedId();
        var dataNeed = calculationService.calculate(dataNeedId, permissionRequest.created());
        switch (dataNeed) {
            case ValidatedHistoricalDataDataNeedResult ignored when isActiveAndNeedsToBeFetched(permissionRequest) ->
                    pollTimeSeriesData(permissionRequest);
            case ValidatedHistoricalDataDataNeedResult ignored ->
                    LOGGER.info("Permission request {} is not active yet", permissionId);
            case AccountingPointDataNeedResult ignored -> retrieveAccountPointData(permissionRequest);
            default -> LOGGER.info(
                    "Permission request {} with data need {} does not need polling",
                    permissionId,
                    dataNeedId
            );
        }
    }

    @Override
    public void pollTimeSeriesData(CdsPermissionRequest pr) {
        var start = pr.oldestMeterReading().orElseGet(() -> pr.start().atStartOfDay(ZoneOffset.UTC));
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var prEnd = endOfDay(pr.end(), ZoneOffset.UTC);
        var end = prEnd.isBefore(now) ? prEnd : now;
        pollTimeSeriesData(pr, start, end).subscribe();
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(CdsPermissionRequest permissionRequest) {
        var today = LocalDate.now(ZoneOffset.UTC);
        if (permissionRequest.start().isAfter(today)) {
            return false;
        }
        return permissionRequest.latestMeterReadingEndDate().map(today::isAfter).orElse(true);
    }

    public Mono<Tuple5<
            List<AccountsEndpoint200ResponseAllOfAccountsInner>,
            List<ServiceContractEndpoint200ResponseAllOfServiceContractsInner>,
            List<ServicePointEndpoint200ResponseAllOfServicePointsInner>,
            List<MeterDeviceEndpoint200ResponseAllOfMeterDevicesInner>,
            List<UsageSegmentEndpoint200ResponseAllOfUsageSegmentsInner>
            >> pollTimeSeriesData(
            CdsPermissionRequest pr,
            ZonedDateTime start,
            ZonedDateTime end
    ) {
        var permissionId = pr.permissionId();
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        var client = factory.get(pr);
        return client.validatedHistoricalData(pr, end, start)
                     .doOnError(errorHandler, errorHandler.thenRevoke(pr))
                     .doOnSuccess(res -> streams.publishValidatedHistoricalData(
                             pr, res.getT1(), res.getT2(), res.getT3(), res.getT4(), res.getT5()
                     ));
    }

    private void retrieveAccountPointData(CdsPermissionRequest pr) {
        var permissionId = pr.permissionId();
        LOGGER.info("Polling accounting point data for permission request {}", permissionId);
        var client = factory.get(pr);
        client.accountingPointData(pr)
              .doOnError(errorHandler, errorHandler.thenRevoke(pr))
              .subscribe(res -> streams.publishAccountingPointData(
                      pr, res.getT1(), res.getT2(), res.getT3(), res.getT4()
              ));
    }
}
