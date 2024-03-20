package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.filter.MeteringDataFilter;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class DataApiService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApiService.class);
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink;


    public DataApiService(DataApi dataApi, Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink) {
        this.dataApi = dataApi;
        this.identifiableMeteringDataSink = identifiableMeteringDataSink;
    }

    public void fetchDataForPermissionRequest(EsPermissionRequest permissionRequest, LocalDate start, LocalDate end) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Polling metering data for permission request {} from {} to {}", permissionRequest.permissionId(), start, end);
        }
        tryGetConsumptionKwh(
                MeteringDataRequest.fromPermissionRequest(permissionRequest, start, end),
                permissionRequest
        );
    }

    private void tryGetConsumptionKwh(MeteringDataRequest request, EsPermissionRequest permissionRequest) {
        dataApi.getConsumptionKwh(request)
                .map(IntermediateMeteringData::fromMeteringData)
                .flatMap(result -> new MeteringDataFilter(result, permissionRequest).filter())
                .map(result -> new IdentifiableMeteringData(permissionRequest, result))
                .doOnError(e -> retryOrRevoke(request, permissionRequest, e))
                .subscribe(identifiableMeteringDataSink::tryEmitNext);
    }

    private void retryOrRevoke(MeteringDataRequest request, EsPermissionRequest permissionRequest, Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) { // do match the exception we need to get the cause
            cause = cause.getCause();
        }

        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("Error polling for metering data for permission request {}", permissionRequest.permissionId(), cause);
        }

        if (cause instanceof DatadisApiException exception) {
            if (exception.statusCode() == HttpStatus.FORBIDDEN.value()) {
                try {
                    permissionRequest.revoke();
                } catch (StateTransitionException ex) {
                    LOGGER.warn("Error revoking permission request", ex);
                }
            }
            if (exception.statusCode() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                request = request.minusMonths(1);
                if (!request.startDate().isBefore(LocalDate.now(ZONE_ID_SPAIN).minusMonths(MAXIMUM_MONTHS_IN_THE_PAST))) {
                    tryGetConsumptionKwh(request, permissionRequest);
                }
            }
        }
    }

    @Override
    public void close() {
        identifiableMeteringDataSink.tryEmitComplete();
    }
}
