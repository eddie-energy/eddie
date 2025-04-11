package energy.eddie.regionconnector.cds.services;

import energy.eddie.api.agnostic.data.needs.AccountingPointDataNeedResult;
import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientErrorHandler;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientFactory;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequest;
import energy.eddie.regionconnector.cds.providers.IdentifiableDataStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.shared.utils.DateTimeUtils.endOfDay;

@Service
public class PollingService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PollingService.class);
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final CustomerDataClientFactory factory;
    private final IdentifiableDataStreams streams;
    private final CustomerDataClientErrorHandler errorHandler;

    public PollingService(
            DataNeedCalculationService<DataNeed> calculationService,
            CustomerDataClientFactory factory,
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
            case ValidatedHistoricalDataDataNeedResult ignored -> retrieveValidatedHistoricalData(permissionRequest);
            case AccountingPointDataNeedResult ignored -> retrieveAccountPointData(permissionRequest);
            default -> LOGGER.info(
                    "Permission request {} with data need {} does not need polling for validated historical data",
                    permissionId,
                    dataNeedId
            );
        }
    }

    public void retrieveValidatedHistoricalData(CdsPermissionRequest pr) {
        var permissionId = pr.permissionId();
        LOGGER.info("Checking if permission request {} is active and needs validated historical data polled",
                    permissionId);
        if (pr.start().isAfter(LocalDate.now(ZoneOffset.UTC))) {
            LOGGER.info("Permission request {} is not active yet", permissionId);
            return;
        }
        LOGGER.info("Polling validated historical data for permission request {}", permissionId);
        var start = pr.oldestMeterReading().orElseGet(() -> pr.start().atStartOfDay(ZoneOffset.UTC));
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        var prEnd = endOfDay(pr.end(), ZoneOffset.UTC);
        var end = prEnd.isBefore(now) ? prEnd : now;
        var client = factory.get(pr);
        Mono.zip(
                    client.accounts(pr),
                    client.serviceContracts(pr),
                    client.servicePoints(pr),
                    client.meterDevices(pr),
                    client.usagePoints(pr, end, start)
            )
            .doOnError(errorHandler, errorHandler.thenRevoke(pr))
            .subscribe(res -> streams.publishValidatedHistoricalData(
                    pr, res.getT1(), res.getT2(), res.getT3(), res.getT4(), res.getT5()
            ));
    }

    private void retrieveAccountPointData(CdsPermissionRequest pr) {
        var permissionId = pr.permissionId();
        LOGGER.info("Polling accounting point data for permission request {}", permissionId);
        var client = factory.get(pr);
        Mono.zip(
                    client.accounts(pr),
                    client.serviceContracts(pr),
                    client.servicePoints(pr),
                    client.meterDevices(pr)
            )
            .doOnError(errorHandler, errorHandler.thenRevoke(pr))
            .subscribe(res -> streams.publishAccountingPointData(
                    pr, res.getT1(), res.getT2(), res.getT3(), res.getT4()
            ));
    }
}
