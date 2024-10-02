package energy.eddie.regionconnector.dk.energinet.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.filter.MeteringDetailsApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.ApiCredentials;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@Service
public class AccountingPointDetailsService implements AutoCloseable {
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(error -> error instanceof WebClientResponseException.TooManyRequests || error instanceof WebClientResponseException.ServiceUnavailable);
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDetailsService.class);

    public final MeteringDetailsApiResponseFilter meteringDetailsApiResponseFilter = new MeteringDetailsApiResponseFilter();
    private final Sinks.Many<IdentifiableAccountingPointDetails> sink = Sinks.many().multicast().onBackpressureBuffer();
    private final Flux<IdentifiableAccountingPointDetails> identifiableMeteringPointDetailsFlux;
    private final EnerginetCustomerApi energinetCustomerApi;
    private final ObjectMapper objectMapper;
    private final Outbox outbox;
    private final ApiExceptionService apiExceptionService;


    public AccountingPointDetailsService(
            EnerginetCustomerApi energinetCustomerApi,
            ObjectMapper objectMapper,
            Outbox outbox,
            ApiExceptionService apiExceptionService
    ) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.objectMapper = objectMapper;
        this.outbox = outbox;
        this.apiExceptionService = apiExceptionService;
        this.identifiableMeteringPointDetailsFlux = sink.asFlux().share();
    }


    public void fetchMeteringPointDetails(DkEnerginetPermissionRequest permissionRequest) {
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);


        ApiCredentials apiCredentials = new ApiCredentials(
                energinetCustomerApi,
                permissionRequest.refreshToken(),
                permissionRequest.accessToken(),
                objectMapper
        );

        apiCredentials
                .accessToken()
                .flatMap(token -> energinetCustomerApi.getMeteringPointDetails(meteringPointsRequest, token))
                .retryWhen(RETRY_BACKOFF_SPEC)
                .flatMap(response -> meteringDetailsApiResponseFilter.filter(
                        permissionRequest.meteringPoint(),
                        response
                ))
                .map(meteringPointDetailsCustomerDto -> new IdentifiableAccountingPointDetails(
                        permissionRequest,
                        meteringPointDetailsCustomerDto
                ))
                .doOnError(error -> apiExceptionService.handleError(permissionRequest.permissionId(), error))
                .onErrorComplete()
                .subscribe(this::handleIdentifiableMeteringPointDetails);
    }

    public Flux<IdentifiableAccountingPointDetails> identifiableMeteringPointDetailsFlux() {
        return identifiableMeteringPointDetailsFlux;
    }

    @Override
    public void close() {
        sink.emitComplete(Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1)));
    }

    private void handleIdentifiableMeteringPointDetails(IdentifiableAccountingPointDetails identifiableAccountingPointDetails) {
        String permissionId = identifiableAccountingPointDetails.permissionRequest().permissionId();

        LOGGER.info("Fetched metering point details from Energinet for permission request {}", permissionId);

        sink.emitNext(
                identifiableAccountingPointDetails,
                Sinks.EmitFailureHandler.busyLooping(Duration.ofMinutes(1))
        );

        outbox.commit(new DkSimpleEvent(
                permissionId,
                PermissionProcessStatus.FULFILLED
        ));
    }
}
