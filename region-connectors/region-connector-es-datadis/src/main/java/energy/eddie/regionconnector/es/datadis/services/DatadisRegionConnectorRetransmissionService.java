package energy.eddie.regionconnector.es.datadis.services;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.api.agnostic.retransmission.RetransmissionRequest;
import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.es.datadis.api.DataApi;
import energy.eddie.regionconnector.es.datadis.api.DatadisApiException;
import energy.eddie.regionconnector.es.datadis.dtos.IntermediateMeteringData;
import energy.eddie.regionconnector.es.datadis.dtos.MeteringDataRequest;
import energy.eddie.regionconnector.es.datadis.filter.MeteringDataFilter;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.persistence.EsPermissionRequestRepository;
import energy.eddie.regionconnector.es.datadis.providers.agnostic.IdentifiableMeteringData;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;

import static energy.eddie.regionconnector.es.datadis.DatadisRegionConnectorMetadata.ZONE_ID_SPAIN;

@Service
public class DatadisRegionConnectorRetransmissionService implements RegionConnectorRetransmissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatadisRegionConnectorRetransmissionService.class);

    private final EsPermissionRequestRepository repository;
    private final DataNeedsService dataNeedsService;
    private final DataApi dataApi;
    private final Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink;

    public DatadisRegionConnectorRetransmissionService(
            EsPermissionRequestRepository repository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            DataApi dataApi,
            Sinks.Many<IdentifiableMeteringData> identifiableMeteringDataSink
    ) {
        this.repository = repository;
        this.dataNeedsService = dataNeedsService;
        this.dataApi = dataApi;
        this.identifiableMeteringDataSink = identifiableMeteringDataSink;
    }

    @Override
    public Mono<RetransmissionResult> requestRetransmission(RetransmissionRequest retransmissionRequest) {
        LOGGER.info("Validating retransmission request {}", retransmissionRequest);
        var permissionRequest = repository.findByPermissionId(retransmissionRequest.permissionId());
        ZonedDateTime now = ZonedDateTime.now(ZONE_ID_SPAIN);

        if (permissionRequest.isEmpty()) {
            LOGGER.warn("No permission with this id found: {}", retransmissionRequest.permissionId());
            return Mono.just(new PermissionRequestNotFound(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        var request = permissionRequest.get();

        if (request.status() != PermissionProcessStatus.ACCEPTED && request.status() != PermissionProcessStatus.FULFILLED) {
            LOGGER.warn("Can only request retransmission for accepted or fulfilled permissions, current status: {}",
                        request.status());
            return Mono.just(new NoActivePermission(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        var dataNeed = dataNeedsService.getById(request.dataNeedId());
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed)) {
            var reason = "Retransmission of data for " + dataNeed.getClass().getSimpleName() + " not supported";
            LOGGER.warn(reason);
            return Mono.just(new NotSupported(
                    retransmissionRequest.permissionId(),
                    now,
                    reason
            ));
        }

        if (retransmissionRequest.from().isBefore(request.start()) ||
            retransmissionRequest.to().isAfter(request.end())
        ) {
            LOGGER.warn("Retransmission request not within permission time frame");
            return Mono.just(new NoPermissionForTimeFrame(
                    retransmissionRequest.permissionId(),
                    now
            ));
        }

        if (now.toLocalDate().isEqual(retransmissionRequest.to()) ||
            now.toLocalDate().isBefore(retransmissionRequest.to())
        ) {
            LOGGER.warn("Retransmission request to date needs to be before today!");
            return Mono.just(new NotSupported(
                    retransmissionRequest.permissionId(),
                    now,
                    "Retransmission to date needs to be before today"
            ));
        }

        return requestValidatedHistoricalData(retransmissionRequest, request);
    }

    private @NotNull Mono<RetransmissionResult> requestValidatedHistoricalData(
            RetransmissionRequest retransmissionRequest,
            EsPermissionRequest request
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
