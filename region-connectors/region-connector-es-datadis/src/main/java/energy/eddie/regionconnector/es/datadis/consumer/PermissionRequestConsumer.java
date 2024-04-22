package energy.eddie.regionconnector.es.datadis.consumer;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.AccountingPointData;
import energy.eddie.regionconnector.es.datadis.permission.request.DistributorCode;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.services.HistoricalDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestConsumer.class);
    private final HistoricalDataService historicalDataService;

    public PermissionRequestConsumer(HistoricalDataService historicalDataService) {
        this.historicalDataService = historicalDataService;
    }

    public void acceptPermission(
            EsPermissionRequest permissionRequest,
            AccountingPointData accountingPointData
    ) {
        permissionRequest.setDistributorCodePointTypeAndProductionSupport(
                DistributorCode.fromCode(accountingPointData.supply().distributorCode()),
                accountingPointData.supply().pointType(),
                accountingPointData.contractDetails().installedCapacity().isPresent()
        );
        try {
            permissionRequest.accept();
            historicalDataService.fetchAvailableHistoricalData(permissionRequest);
        } catch (StateTransitionException e) {
            LOGGER.error("Error accepting permission request", e);
        }
    }

    public void consumeError(Throwable e, EsPermissionRequest permissionRequest) {
        Throwable cause = e;
        while (cause.getCause() != null) { // do match the exception we need to get the cause
            cause = cause.getCause();
        }
        LOGGER.error("Error while retrieving permission request supply", e);
        try {
            if (cause instanceof DatadisApiException datadisApiException && datadisApiException.statusCode() == HttpStatus.FORBIDDEN.value()) {
                permissionRequest.timeOut(); // we never actually got permission, so we should time out
            } else {
                permissionRequest.invalid();
            }
        } catch (StateTransitionException ex) {
            LOGGER.error("Error invalidating permission request", ex);
        }
    }
}
