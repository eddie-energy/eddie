package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.dk.energinet.customer.client.EnerginetCustomerApiClient;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.filter.MeteringDetailsApiResponseFilter;
import energy.eddie.regionconnector.dk.energinet.permission.events.DkSimpleEvent;
import energy.eddie.regionconnector.dk.energinet.permission.request.ApiCredentials;
import energy.eddie.regionconnector.dk.energinet.permission.request.api.DkEnerginetPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.providers.EnergyDataStreams;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableAccountingPointDetails;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

@Service
public class AccountingPointDetailsService {
    public static final RetryBackoffSpec RETRY_BACKOFF_SPEC = Retry.backoff(10, Duration.ofMinutes(1))
                                                                   .filter(error -> error instanceof WebClientResponseException.TooManyRequests || error instanceof WebClientResponseException.ServiceUnavailable);
    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPointDetailsService.class);

    public final MeteringDetailsApiResponseFilter meteringDetailsApiResponseFilter = new MeteringDetailsApiResponseFilter();
    private final EnerginetCustomerApiClient energinetCustomerApi;
    private final ObjectMapper objectMapper;
    private final Outbox outbox;
    private final ApiExceptionService apiExceptionService;
    private final EnergyDataStreams streams;


    public AccountingPointDetailsService(
            EnerginetCustomerApiClient energinetCustomerApi,
            ObjectMapper objectMapper,
            Outbox outbox,
            ApiExceptionService apiExceptionService,
            EnergyDataStreams streams
    ) {
        this.energinetCustomerApi = energinetCustomerApi;
        this.objectMapper = objectMapper;
        this.outbox = outbox;
        this.apiExceptionService = apiExceptionService;
        this.streams = streams;
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

    private void handleIdentifiableMeteringPointDetails(IdentifiableAccountingPointDetails identifiableAccountingPointDetails) {
        String permissionId = identifiableAccountingPointDetails.permissionRequest().permissionId();

        LOGGER.info("Fetched metering point details from Energinet for permission request {}", permissionId);

        streams.publish(identifiableAccountingPointDetails);

        outbox.commit(new DkSimpleEvent(
                permissionId,
                PermissionProcessStatus.FULFILLED
        ));
    }
}
