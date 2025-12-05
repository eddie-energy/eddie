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
import energy.eddie.regionconnector.es.datadis.providers.EnergyDataStreams;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableAccountingPointData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestConsumer.class);
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final EnergyDataStreams streams;

    public PermissionRequestConsumer(
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected from another spring context
            DataNeedsService dataNeedsService,
            EnergyDataStreams streams
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
        this.streams = streams;
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

    private void handleAccountingPointDataNeed(
            AccountingPointData accountingPointData,
            EsPermissionRequest permissionRequest
    ) {
        outbox.commit(new EsAcceptedEventForAPD(permissionRequest.permissionId()));
        streams.publish(new IdentifiableAccountingPointData(permissionRequest, accountingPointData));
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
}
