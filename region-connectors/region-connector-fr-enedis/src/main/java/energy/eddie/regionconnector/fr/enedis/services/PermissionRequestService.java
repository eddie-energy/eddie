package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.StateTransitionException;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnector.ZONE_ID_FR;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.PERIOD_EARLIEST_START;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.PERIOD_LATEST_END;

@Service
public class PermissionRequestService {
    private final JpaPermissionRequestRepository repository;
    private final PermissionRequestFactory factory;
    private final EnedisConfiguration configuration;
    private final HistoricalDataService historicalDataService;
    private final DataNeedsService dataNeedsService;

    public PermissionRequestService(
            JpaPermissionRequestRepository repository,
            PermissionRequestFactory factory,
            EnedisConfiguration configuration,
            HistoricalDataService historicalDataService,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService
    ) {
        this.repository = repository;
        this.factory = factory;
        this.configuration = configuration;
        this.historicalDataService = historicalDataService;
        this.dataNeedsService = dataNeedsService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws StateTransitionException, DataNeedNotFoundException, UnsupportedDataNeedException {
        var referenceDate = LocalDate.now(ZONE_ID_FR);
        var wrapper = dataNeedsService.findDataNeedAndCalculateStartAndEnd(permissionRequestForCreation.dataNeedId(),
                                                                           referenceDate,
                                                                           PERIOD_EARLIEST_START,
                                                                           PERIOD_LATEST_END);

        if (!(wrapper.timeframedDataNeed() instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   permissionRequestForCreation.dataNeedId(),
                                                   "This region connector only supports validated historical data data needs.");
        }

        var granularity = switch (vhdDataNeed.minGranularity()) {
            case PT30M, P1D -> vhdDataNeed.minGranularity();
            default -> throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                              permissionRequestForCreation.dataNeedId(),
                                                              "Unsupported granularity: '" + vhdDataNeed.minGranularity() + "'");
        };

        FrEnedisPermissionRequest permissionRequest = factory.create(permissionRequestForCreation,
                                                                     wrapper.calculatedStart(),
                                                                     wrapper.calculatedEnd(),
                                                                     granularity);
        permissionRequest.validate();
        URI redirectUri = buildRedirectUri(permissionRequest);
        permissionRequest.sendToPermissionAdministrator();
        return new CreatedPermissionRequest(permissionRequest.permissionId(), redirectUri);
    }

    public void authorizePermissionRequest(
            String permissionId,
            String usagePointId
    ) throws StateTransitionException, PermissionNotFoundException {
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
