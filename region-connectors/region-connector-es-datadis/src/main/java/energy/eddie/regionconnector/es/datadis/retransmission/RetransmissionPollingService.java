package energy.eddie.regionconnector.es.datadis.retransmission;

import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.DataNotAvailable;
import energy.eddie.api.agnostic.retransmission.result.Failure;
import energy.eddie.api.agnostic.retransmission.result.RetransmissionResult;
import energy.eddie.api.agnostic.retransmission.result.Success;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.filter.MeteringDataFilter;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import energy.eddie.regionconnector.shared.retransmission.PollingFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class RetransmissionPollingService implements PollingFunction<EsPermissionRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetransmissionPollingService.class);
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink;

    public RetransmissionPollingService(
            DataApi dataApi,
            Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink
    ) {
        this.dataApi = dataApi;
        this.identifiableMeteringDataSink = identifiableMeteringDataSink;
    }

    @Override
    public Mono<RetransmissionResult> poll(
            EsPermissionRequest request,
            RetransmissionRequest retransmissionRequest
    ) {
        LOGGER.info("Requesting retransmission of {}", retransmissionRequest);
        var meteringDataRequest = MeteringDataRequest.fromPermissionRequest(
                request,
                retransmissionRequest.from(),
                retransmissionRequest.to()
        );

        return dataApi
                .getConsumptionKwh(meteringDataRequest)
                .flatMap(IntermediateMeteringData::fromMeteringData)
                .flatMap(result -> new MeteringDataFilter(result, request)
                        .filter(retransmissionRequest.from(), retransmissionRequest.to())
                )
                .map(filteredData -> new IdentifiableMeteringData(request, filteredData))
                .map(meteringData -> emitMeteringDataAndMapToRetransmissionResult(meteringData, request))
                .defaultIfEmpty(
                        new DataNotAvailable(request.permissionId(), ZonedDateTime.now(ZONE_ID_SPAIN))
                ) // this occurs when the api returns 200 but no data or only data outside the filter
                .onErrorResume(error -> mapErrorToRetransmissionResult(error, request, retransmissionRequest));
    }

    private RetransmissionResult emitMeteringDataAndMapToRetransmissionResult(
            IdentifiableMeteringData meteringData,
            EsPermissionRequest request
    ) {
        LOGGER.atInfo()
              .addArgument(request::permissionId)
              .log("Received data for retransmission of permission {}");
        var emitResult = identifiableMeteringDataSink.tryEmitNext(meteringData);
        if (emitResult.isFailure()) {
            LOGGER.atError()
                  .addArgument(request::permissionId)
                  .log("Failed to emit data for retransmission of permission {}");
            return new Failure(request.permissionId(),
                               ZonedDateTime.now(ZONE_ID_SPAIN),
                               "Could not emit fetched data");
        }
        return new Success(request.permissionId(), ZonedDateTime.now(ZONE_ID_SPAIN));
    }

    private static Mono<Failure> mapErrorToRetransmissionResult(
            Throwable error,
            EsPermissionRequest request,
            RetransmissionRequest retransmissionRequest
    ) {
        LOGGER.atError()
              .addArgument(retransmissionRequest)
              .addArgument(error::getMessage)
              .setCause(error)
              .log("Error while trying to fetch data for retransmission request {}: {}");

        if (error instanceof DatadisApiException apiException && apiException.statusCode() == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return Mono.just(new Failure(
                    request.permissionId(),
                    ZonedDateTime.now(ZONE_ID_SPAIN),
                    "Datadis returned: '429 Too Many Requests'. Try again in 24 hours."
            ));
        }

        return Mono.just(new Failure(request.permissionId(), ZonedDateTime.now(ZONE_ID_SPAIN), error.getMessage()));
    }
}
