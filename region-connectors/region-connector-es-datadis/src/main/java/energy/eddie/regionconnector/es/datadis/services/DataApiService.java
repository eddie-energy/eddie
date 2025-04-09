package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.filter.MeteringDataFilter;
import energy.eddie.regionconnector.es.datadis.permission.events.EsSimpleEvent;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.services.CommonPollingService;
import energy.eddie.regionconnector.shared.services.MeterReadingPermissionUpdateAndFulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDate;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.MAXIMUM_MONTHS_IN_THE_PAST;
import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class DataApiService implements AutoCloseable, CommonPollingService<EsPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataApiService.class);
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink;
    private final MeterReadingPermissionUpdateAndFulfillmentService permissionUpdateAndFulfillmentService;
    private final Outbox outbox;


    public DataApiService(
            DataApi dataApi,
            Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink,
            MeterReadingPermissionUpdateAndFulfillmentService meterReadingPermissionUpdateAndFulfillmentService,
            Outbox outbox
    ) {
        this.dataApi = dataApi;
        this.identifiableMeteringDataSink = identifiableMeteringDataSink;
        this.permissionUpdateAndFulfillmentService = meterReadingPermissionUpdateAndFulfillmentService;
        this.outbox = outbox;
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
                .flatMap(IntermediateMeteringData::fromMeteringData)
                .flatMap(result -> new MeteringDataFilter(result, permissionRequest).filter())
                .map(result -> new IdentifiableMeteringData(permissionRequest, result))
                .doOnError(e -> retryOrRevoke(request, permissionRequest, e))
                .onErrorComplete() // The error is handled by doOnError, so we can complete the stream here
                .subscribe(identifiableMeteringData -> handleIdentifiableMeteringData(permissionRequest,
                        identifiableMeteringData));
    }

    private void handleIdentifiableMeteringData(
            EsPermissionRequest permissionRequest,
            IdentifiableMeteringData identifiableMeteringData
    ) {
        permissionUpdateAndFulfillmentService.tryUpdateAndFulfillPermissionRequest(
                permissionRequest,
                identifiableMeteringData
        );
        identifiableMeteringDataSink.emitNext(identifiableMeteringData,
                Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
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
                outbox.commit(new EsSimpleEvent(permissionRequest.permissionId(), PermissionProcessStatus.REVOKED));
            }
            if (exception.statusCode() == HttpStatus.TOO_MANY_REQUESTS.value()) {
                request = request.minusMonths(1);
                if (!request.startDate()
                        .isBefore(LocalDate.now(ZONE_ID_SPAIN).minusMonths(MAXIMUM_MONTHS_IN_THE_PAST))) {
                    tryGetConsumptionKwh(request, permissionRequest);
                }
            }
        }
    }

    @Override
    public void close() {
        identifiableMeteringDataSink.tryEmitComplete();
    }

    @Override
    public void pollTimeSeriesData(EsPermissionRequest permissionRequest) {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        LocalDate yesterday = today.minusDays(1);
        LocalDate lastPulledOrStart = permissionRequest.latestMeterReadingEndDate().orElse(permissionRequest.start());
        LocalDate startDate = lastPulledOrStart.isBefore(yesterday) ? lastPulledOrStart : yesterday;

        fetchDataForPermissionRequest(permissionRequest, startDate, yesterday);
    }

    @Override
    public boolean isActiveAndNeedsToBeFetched(EsPermissionRequest permissionRequest) {
        LocalDate today = LocalDate.now(ZONE_ID_SPAIN);
        return permissionRequest.start().isBefore(today);
    }
}
