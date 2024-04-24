package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.events.EsAcceptedEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsInvalidEvent;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestConsumer.class);
    private final Outbox outbox;

    public PermissionRequestConsumer(Outbox outbox) {
        this.outbox = outbox;
    }

    public void acceptPermission(
            EsPermissionRequest permissionRequest,
            AccountingPointData accountingPointData
    ) {
        var supply = accountingPointData.supply();
        outbox.commit(
                new EsAcceptedEvent(permissionRequest.permissionId(),
                                    DistributorCode.fromCode(supply.distributorCode()),
                                    supply.pointType(),
                                    accountingPointData.contractDetails().installedCapacity().isPresent())
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
}
