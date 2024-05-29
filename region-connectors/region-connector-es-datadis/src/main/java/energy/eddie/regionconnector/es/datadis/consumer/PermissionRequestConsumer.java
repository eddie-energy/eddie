package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForAPD;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEventForVHD;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInvalidEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Component
public class PermissionRequestConsumer implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestConsumer.class);
    private final Outbox outbox;
    private final Sinks.Many<IdentifiableAccountingPointData> identifiableAccountingPointDataSink;
    private final DataNeedsService dataNeedsService;

    public PermissionRequestConsumer(
            Outbox outbox,
            Sinks.Many<IdentifiableAccountingPointData> identifiableAccountingPointDataSink,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from another spring context
            DataNeedsService dataNeedsService
    ) {
        this.outbox = outbox;
        this.identifiableAccountingPointDataSink = identifiableAccountingPointDataSink;
        this.dataNeedsService = dataNeedsService;
    }

    public void acceptPermission(
            EsPermissionRequest permissionRequest,
            AccountingPointData accountingPointData
    ) {
        var dataNeed = dataNeedsService.getById(permissionRequest.dataNeedId());

        switch (dataNeed) {
            case AccountingPointDataNeed ignored ->
                    handleAccountingPointDataNeed(accountingPointData, permissionRequest);
            default -> handleHistoricalValidatedDataDataNeed(accountingPointData, permissionRequest);
        }
    }

    private void handleAccountingPointDataNeed(
            AccountingPointData accountingPointData,
            EsPermissionRequest permissionRequest
    ) {
        outbox.commit(new EsAcceptedEventForAPD(permissionRequest.permissionId()));
        identifiableAccountingPointDataSink.emitNext(
                new IdentifiableAccountingPointData(permissionRequest, accountingPointData),
                Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1))
        );
        outbox.commit(new EsSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.FULFILLED));
    }

    private void handleHistoricalValidatedDataDataNeed(
            AccountingPointData accountingPointData,
            EsPermissionRequest permissionRequest
    ) {
        var supply = accountingPointData.supply();
        outbox.commit(
                new EsAcceptedEventForVHD(
                        permissionRequest.permissionId(),
                        DistributorCode.fromCode(supply.distributorCode()),
                        supply.pointType(),
                        accountingPointData.contractDetails().installedCapacity().isPresent()
                )
        );
    }

    public void consumeError(Throwable e, EsPermissionRequest permissionRequest) {
        Throwable cause = e;
        while (cause.getCause() != null) { // do match the exception we need to get the cause
            cause = cause.getCause();
        }
        LOGGER.warn("Error while retrieving permission request supply", e);
        if (cause instanceof DatadisApiException datadisApiException && datadisApiException.statusCode() == HttpStatus.FORBIDDEN.value()) {
            // we never actually got permission, so we should time out
            outbox.commit(new EsSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.TIMED_OUT));
        } else {
            outbox.commit(new EsInvalidEvent(permissionRequest.permissionId(),
                                             cause.getMessage() == null ? "" : cause.getMessage()));
        }
    }

    @Override
    public void close() {
        identifiableAccountingPointDataSink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }
}
