package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.PermissionRequestRepository;
import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.agnostic.process.model.TimeframedPermissionRequest;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.permission.request.models.FutureDataPermission;
import energy.eddie.regionconnector.fr.enedis.permission.request.repositories.FutureDataPermissionRepository;
import energy.eddie.regionconnector.fr.enedis.utils.EnedisDuration;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class PermissionRequestService {
    private static final String TIME_FRAME_NOT_SUPPORTED = "Permission Requests with a timeframe from past to future not supported.";
    private final PermissionRequestRepository<TimeframedPermissionRequest> repository;
    private final PermissionRequestFactory factory;
    private final EnedisConfiguration configuration;
    private final PollingService pollingService;
    private final FutureDataPermissionRepository futureDataPermissionRepository;

    public PermissionRequestService(PermissionRequestRepository<TimeframedPermissionRequest> repository, PermissionRequestFactory factory, EnedisConfiguration configuration, PollingService pollingService, FutureDataPermissionRepository futureDataPermissionRepository) {
        this.repository = repository;
        this.factory = factory;
        this.configuration = configuration;
        this.pollingService = pollingService;
        this.futureDataPermissionRepository = futureDataPermissionRepository;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws StateTransitionException {
        TimeframedPermissionRequest permissionRequest = factory.create(permissionRequestForCreation);
        permissionRequest.validate();
        URI redirectUri = buildRedirectUri(permissionRequest);
        permissionRequest.sendToPermissionAdministrator();
        return new CreatedPermissionRequest(permissionRequest.permissionId(), redirectUri);
    }

    public void authorizePermissionRequest(String permissionId, String usagePointId) throws StateTransitionException, PermissionNotFoundException {
        Optional<TimeframedPermissionRequest> optionalPermissionRequest = findPermissionRequestByPermissionId(permissionId);
        if (optionalPermissionRequest.isEmpty()) {
            // unknown state / permissionId => not coming / initiated by our frontend
            throw new PermissionNotFoundException(permissionId);
        }

        TimeframedPermissionRequest permissionRequest = optionalPermissionRequest.get();
        permissionRequest.receivedPermissionAdministratorResponse();
        if (usagePointId == null) { // probably when request was denied
            permissionRequest.reject();
        } else {
            permissionRequest.accept();
            LocalDate now = LocalDate.now(ZoneId.of("Europe/Paris"));

            // Check if the permission request is in the past, future or both
            if (permissionRequest.end() == null) {
                if (permissionRequest.start().toLocalDate().isAfter(now)) {
                    createFutureDataPermission(permissionRequest, usagePointId);
                } else {
                    // TODO: Split permission request
                    throw new UnsupportedOperationException(TIME_FRAME_NOT_SUPPORTED);
                }
            } else {
                if (permissionRequest.end().toLocalDate().isBefore(now)) {
                    retrieveConsumptionData(permissionRequest, usagePointId);
                } else if (permissionRequest.start().toLocalDate().isAfter(now)) {
                    createFutureDataPermission(permissionRequest, usagePointId);
                } else {
                    // TODO: Split permission request
                    throw new UnsupportedOperationException(TIME_FRAME_NOT_SUPPORTED);
                }
            }
        }
    }

    private void retrieveConsumptionData(TimeframedPermissionRequest permissionRequest, String usagePointId) {
        pollingService.requestData(permissionRequest, usagePointId);
    }

    private void createFutureDataPermission(TimeframedPermissionRequest permissionRequest, String usagePointId) {
        var futureDataPermission = new FutureDataPermission().withMeteringPointId(usagePointId).withValidFrom(permissionRequest.start()).withValidTo(permissionRequest.end());
        futureDataPermissionRepository.save(futureDataPermission);
    }

    private URI buildRedirectUri(TimeframedPermissionRequest permissionRequest) {
        try {
            return new URIBuilder()
                    .setScheme("https")
                    .setHost("mon-compte-particulier.enedis.fr")
                    .setPath("/dataconnect/v1/oauth2/authorize")
                    .addParameter("client_id", configuration.clientId())
                    .addParameter("response_type", "code")
                    .addParameter("state", permissionRequest.permissionId())
                    .addParameter("duration", new EnedisDuration(permissionRequest).toString())
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to create redirect URI");
        }
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId).map(request -> new ConnectionStatusMessage(request.connectionId(), request.permissionId(), request.dataNeedId(), null, request.state().status()));
    }

    public Optional<TimeframedPermissionRequest> findPermissionRequestByPermissionId(String permissionId) {
        return repository.findByPermissionId(permissionId)
                .map(factory::create);
    }
}