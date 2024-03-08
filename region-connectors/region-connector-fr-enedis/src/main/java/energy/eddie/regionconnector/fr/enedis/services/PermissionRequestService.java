package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.request.PermissionRequestFactory;
import energy.eddie.regionconnector.fr.enedis.permission.request.api.FrEnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.permission.request.persistence.JpaPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.utils.EnedisDuration;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class PermissionRequestService {
    private final JpaPermissionRequestRepository repository;
    private final PermissionRequestFactory factory;
    private final EnedisConfiguration configuration;
    private final HistoricalDataService historicalDataService;

    public PermissionRequestService(JpaPermissionRequestRepository repository,
                                    PermissionRequestFactory factory,
                                    EnedisConfiguration configuration,
                                    HistoricalDataService historicalDataService) {
        this.repository = repository;
        this.factory = factory;
        this.configuration = configuration;
        this.historicalDataService = historicalDataService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws StateTransitionException {
        FrEnedisPermissionRequest permissionRequest = factory.create(permissionRequestForCreation);
        permissionRequest.validate();
        URI redirectUri = buildRedirectUri(permissionRequest);
        permissionRequest.sendToPermissionAdministrator();
        return new CreatedPermissionRequest(permissionRequest.permissionId(), redirectUri);
    }

    public void authorizePermissionRequest(String permissionId, String usagePointId) throws StateTransitionException, PermissionNotFoundException {
        Optional<FrEnedisPermissionRequest> optionalPermissionRequest = findPermissionRequestByPermissionId(permissionId);
        if (optionalPermissionRequest.isEmpty()) {
            // unknown state / permissionId => not coming / initiated by our frontend
            throw new PermissionNotFoundException(permissionId);
        }

        FrEnedisPermissionRequest permissionRequest = optionalPermissionRequest.get();
        permissionRequest.receivedPermissionAdministratorResponse();
        if (usagePointId == null) { // probably when request was denied
            permissionRequest.reject();
        } else {
            permissionRequest.accept();
            permissionRequest.setUsagePointId(usagePointId);
            historicalDataService.fetchHistoricalMeterReadings(permissionRequest);
        }
    }

    private URI buildRedirectUri(FrEnedisPermissionRequest permissionRequest) {
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
        return repository.findById(permissionId)
                .map(request -> new ConnectionStatusMessage(
                        request.connectionId(),
                        request.permissionId(),
                        request.dataNeedId(),
                        null,
                        request.status()));
    }

    public Optional<FrEnedisPermissionRequest> findPermissionRequestByPermissionId(String permissionId) {
        return repository.findById(permissionId)
                .map(factory::create);
    }

    public List<FrEnedisPermissionRequest> findAllAcceptedPermissionRequests() {
        return repository.findAllByStatusIs(PermissionProcessStatus.ACCEPTED)
                .stream()
                .map(factory::create)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<FrEnedisPermissionRequest> findTimedOutPermissionRequests(int timeoutDuration) {
        return repository.findTimedOutPermissionRequests(timeoutDuration).stream()
                .map(factory::create)
                .toList();
    }
}
