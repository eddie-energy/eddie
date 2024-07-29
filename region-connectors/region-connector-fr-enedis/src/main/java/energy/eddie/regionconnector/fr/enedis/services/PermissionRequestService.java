package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.permission.events.*;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.utils.EnedisDuration;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final FrPermissionRequestRepository repository;
    private final EnedisConfiguration configuration;
    private final DataNeedsService dataNeedsService;
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;

    public PermissionRequestService(
            FrPermissionRequestRepository repository,
            EnedisConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        LOGGER.info("Got request to create a new permission, request was: {}", permissionRequestForCreation);
        var permissionId = UUID.randomUUID().toString();

        var dataNeed = dataNeedsService.findById(permissionRequestForCreation.dataNeedId())
                                       .orElseThrow(() -> new DataNeedNotFoundException(permissionRequestForCreation.dataNeedId()));

        outbox.commit(new FrCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         permissionRequestForCreation.dataNeedId()));
        var calculation = calculationService.calculate(dataNeed);
        if (!calculation.supportsDataNeed()
            || calculation.energyDataTimeframe() == null
            || calculation.permissionTimeframe() == null) {
            outbox.commit(new FrMalformedEvent(permissionId,
                                               new AttributeError(DATA_NEED_ID, "Unsupported data need")));
            throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   permissionRequestForCreation.dataNeedId(),
                                                   "This region connector only supports validated historical data data needs.");
        }
        if (calculation.granularities() == null || calculation.granularities().isEmpty()) {
            outbox.commit(new FrMalformedEvent(permissionId,
                                               new AttributeError(DATA_NEED_ID, "Unsupported granularity")));
            throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   permissionRequestForCreation.dataNeedId(),
                                                   "Unsupported granularity");
        }
        outbox.commit(new FrValidatedEvent(permissionId,
                                           calculation.energyDataTimeframe().start(),
                                           calculation.energyDataTimeframe().end(),
                                           calculation.granularities().getFirst()));
        URI redirectUri = buildRedirectUri(permissionId, calculation.permissionTimeframe().end());
        return new CreatedPermissionRequest(permissionId, redirectUri);
    }

    private URI buildRedirectUri(String permissionId, LocalDate end) {
        try {
            return new URIBuilder()
                    .setScheme("https")
                    .setHost("mon-compte-particulier.enedis.fr")
                    .setPath("/dataconnect/v1/oauth2/authorize")
                    .addParameter("client_id", configuration.clientId())
                    .addParameter("response_type", "code")
                    .addParameter("state", permissionId)
                    .addParameter("duration", new EnedisDuration(end).toString())
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to create redirect URI");
        }
    }

    public void authorizePermissionRequest(
            String permissionId,
            String[] usagePointIds
    ) throws PermissionNotFoundException {
        var permissionRequest = repository
                .findByPermissionId(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        var usagePointId = usagePointIds[0];
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        outbox.commit(new FrAcceptedEvent(permissionId, usagePointId));

        for (int i = 1; i < usagePointIds.length; i++) {
            var newPermissionId = UUID.randomUUID().toString();
            outbox.commit(new FrCreatedEvent(
                    newPermissionId,
                    permissionRequest.connectionId(),
                    permissionRequest.dataNeedId()
            ));
            outbox.commit(new FrValidatedEvent(
                    newPermissionId,
                    permissionRequest.start(),
                    permissionRequest.end(),
                    permissionRequest.granularity()
            ));
            outbox.commit(new FrSimpleEvent(newPermissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new FrAcceptedEvent(newPermissionId, usagePointIds[i]));
        }
    }

    public Optional<ConnectionStatusMessage> findConnectionStatusMessageById(String permissionId) {
        return repository.findByPermissionId(permissionId)
                         .map(request -> new ConnectionStatusMessage(
                                 request.connectionId(),
                                 request.permissionId(),
                                 request.dataNeedId(),
                                 null,
                                 request.status()));
    }
}
