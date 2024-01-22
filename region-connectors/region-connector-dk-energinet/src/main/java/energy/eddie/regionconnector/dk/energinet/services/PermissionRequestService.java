package energy.eddie.regionconnector.dk.energinet.services;

import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.SendToPermissionAdministratorException;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.validation.ValidationException;
import energy.eddie.regionconnector.dk.energinet.customer.api.EnerginetCustomerApi;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPoints;
import energy.eddie.regionconnector.dk.energinet.customer.model.MeteringPointsRequest;
import energy.eddie.regionconnector.dk.energinet.customer.model.MyEnergyDataMarketDocumentResponseListApiResponse;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.api.DkEnerginetCustomerPermissionRequestRepository;
import energy.eddie.regionconnector.dk.energinet.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.dk.energinet.providers.agnostic.IdentifiableApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import reactor.core.publisher.Sinks;

import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionRequestService implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private final DkEnerginetCustomerPermissionRequestRepository repository;
    private final PermissionRequestFactory requestFactory;
    private final EnerginetCustomerApi energinetCustomerApi;
    private final Sinks.Many<IdentifiableApiResponse> apiResponseSink;

    public PermissionRequestService(
            DkEnerginetCustomerPermissionRequestRepository repository,
            PermissionRequestFactory requestFactory,
            EnerginetCustomerApi energinetCustomerApi,
            Sinks.Many<IdentifiableApiResponse> apiResponseSink
    ) {
        this.repository = repository;
        this.requestFactory = requestFactory;
        this.energinetCustomerApi = energinetCustomerApi;
        this.apiResponseSink = apiResponseSink;
    }

    private static void revokePermissionRequest(DkEnerginetCustomerPermissionRequest permissionRequest,
                                                Throwable error) {
        if (!(error instanceof HttpClientErrorException.Unauthorized)) {
            LOGGER.warn("Got error while requesting access token", error);
            return;
        }
        try {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Revoking permission request with permission id {}", permissionRequest.permissionId());
            }
            permissionRequest.revoke();
        } catch (StateTransitionException e) {
            LOGGER.warn("Could not revoke permission request", e);
        }
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request ->
                new ConnectionStatusMessage(
                        request.connectionId(),
                        request.permissionId(),
                        request.dataNeedId(),
                        request.dataSourceInformation(),
                        request.state().status())
        );
    }

    public Optional<DkEnerginetCustomerPermissionRequest> findByPermissionId(String permissionId) {
        var permissionRequest = repository.findByPermissionId(permissionId);
        return permissionRequest
                .map(requestFactory::create);
    }

    /**
     * Creates a new {@link PermissionRequest}, validates it and sends it to the permission administrator.
     *
     * @param requestForCreation Dto that contains the necessary information for this permission request.
     * @return The created PermissionRequest
     * @throws ValidationException                    If the {@code requestForCreation} is not valid.
     * @throws SendToPermissionAdministratorException When an error occurs while sending the request to the PA.
     *                                                If {@link SendToPermissionAdministratorException#userFault()} is true, the customer provided an invalid refresh token.
     */
    public PermissionRequest createAndSendPermissionRequest(PermissionRequestForCreation requestForCreation) throws StateTransitionException {
        DkEnerginetCustomerPermissionRequest permissionRequest = requestFactory.create(requestForCreation);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        // if sendToPA doesn't fail, we have a valid refreshToken and can start polling the records in the background
        permissionRequest.receivedPermissionAdministratorResponse();
        permissionRequest.accept();
        fetchConsumptionRecords(permissionRequest);
        return permissionRequest;
    }

    private void fetchConsumptionRecords(DkEnerginetCustomerPermissionRequest permissionRequest) {
        MeteringPoints meteringPoints = new MeteringPoints();
        meteringPoints.addMeteringPointItem(permissionRequest.meteringPoint());
        MeteringPointsRequest meteringPointsRequest = new MeteringPointsRequest().meteringPoints(meteringPoints);
        permissionRequest.accessToken()
                // If we get an 401 Unauthorized error, the refresh token was revoked and the permission request with that
                .doOnError(error -> revokePermissionRequest(permissionRequest, error))
                .flatMap(accessToken -> energinetCustomerApi.getTimeSeries(
                        permissionRequest.start(),
                        permissionRequest.end(),
                        permissionRequest.granularity(),
                        meteringPointsRequest,
                        accessToken,
                        UUID.fromString(permissionRequest.permissionId())
                ))
                .mapNotNull(MyEnergyDataMarketDocumentResponseListApiResponse::getResult)
                .map(response -> new IdentifiableApiResponse(permissionRequest.permissionId(),
                        permissionRequest.connectionId(), permissionRequest.dataNeedId(), response
                ))
                .doOnError(error -> LOGGER.error("Something went wrong while fetching data from Energinet:", error))
                .subscribe(apiResponseSink::tryEmitNext);
    }

    @Override
    public void close() {
        apiResponseSink.tryEmitComplete();
    }
}