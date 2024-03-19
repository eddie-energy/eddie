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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.utils.DatadisSpecificConstants.ZONE_ID_SPAIN;

@Service
public class DataApiService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApiService.class);
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink;
    private final DatadisFulfillmentService fulfillmentService;
    private final LastPulledMeterReadingService lastPulledMeterReadingService;


    public DataApiService(DataApi dataApi, Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink, DatadisFulfillmentService fulfillmentService, LastPulledMeterReadingService lastPulledMeterReadingService) {
        this.dataApi = dataApi;
        this.identifiableMeteringDataSink = identifiableMeteringDataSink;
        this.fulfillmentService = fulfillmentService;
        this.lastPulledMeterReadingService = lastPulledMeterReadingService;
    }


    public void fetchDataForPermissionRequest(EsPermissionRequest permissionRequest, LocalDate start, LocalDate end) {
        LOGGER.atInfo()
                .addArgument(permissionRequest::permissionId)
                .addArgument(start)
                .addArgument(end)
                .log("Polling metering data for permission request {} from {} to {}");

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
                .onErrorComplete() // The error is handled by doOnError, so we can complete the stream here
                .subscribe(identifiableMeteringData -> handleIdentifiableMeteringData(permissionRequest, identifiableMeteringData));
    }

    private void handleIdentifiableMeteringData(EsPermissionRequest permissionRequest, IdentifiableMeteringData identifiableMeteringData) {
        ZonedDateTime meteringDataEndDate = identifiableMeteringData.intermediateMeteringData().end();
        if (lastPulledMeterReadingService.updateLastPulledMeterReading(permissionRequest, meteringDataEndDate)
                && fulfillmentService.isPermissionRequestFulfilledByDate(permissionRequest, meteringDataEndDate)) {
            fulfillmentService.tryFulfillPermissionRequest(permissionRequest);
        }

        identifiableMeteringDataSink.emitNext(identifiableMeteringData, Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    private void retryOrRevoke(MeteringDataRequest request, EsPermissionRequest permissionRequest, Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null) { // do match the exception we need to get the cause
            cause = cause.getCause();
        }

        LOGGER.atError()
                .addArgument(permissionRequest::permissionId)
                .setCause(e)
                .log("Something went wrong while fetching data for permission request {} from Datadis:");

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
