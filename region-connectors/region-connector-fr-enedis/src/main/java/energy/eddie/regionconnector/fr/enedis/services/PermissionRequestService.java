package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
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
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.*;

@Service
public class PermissionRequestService {
    private static final String DATA_NEED_ID = "dataNeedId";
    private final FrPermissionRequestRepository repository;
    private final EnedisConfiguration configuration;
    private final DataNeedsService dataNeedsService;
    private final Outbox outbox;

    public PermissionRequestService(
            FrPermissionRequestRepository repository,
            EnedisConfiguration configuration,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            Outbox outbox
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.dataNeedsService = dataNeedsService;
        this.outbox = outbox;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var referenceDate = LocalDate.now(ZONE_ID_FR);
        var permissionId = UUID.randomUUID().toString();

        var dataNeed = dataNeedsService.findById(permissionRequestForCreation.dataNeedId())
                                       .orElseThrow(() -> new DataNeedNotFoundException(permissionRequestForCreation.dataNeedId()));

        outbox.commit(new FrCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         permissionRequestForCreation.dataNeedId()));
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhdDataNeed)) {
            outbox.commit(new FrMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID, "Unsupported data need"))));
            throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   permissionRequestForCreation.dataNeedId(),
                                                   "This region connector only supports validated historical data data needs.");
        }

        DataNeedWrapper wrapper;
        try {
            wrapper = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(
                    vhdDataNeed,
                    referenceDate,
                    PERIOD_EARLIEST_START,
                    PERIOD_LATEST_END
            );
        } catch (UnsupportedDataNeedException e) {
            outbox.commit(new FrMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID, e.getMessage()))));
            throw e;
        }

        var granularity = switch (vhdDataNeed.minGranularity()) {
            case PT30M, P1D -> vhdDataNeed.minGranularity();
            default -> {
                outbox.commit(new FrMalformedEvent(permissionId,
                                                   List.of(new AttributeError(DATA_NEED_ID,
                                                                              "Unsupported granularity"))));
                throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       permissionRequestForCreation.dataNeedId(),
                                                       "Unsupported granularity: '" + vhdDataNeed.minGranularity() + "'");
            }
        };
        outbox.commit(new FrValidatedEvent(permissionId,
                                           wrapper.calculatedStart(),
                                           wrapper.calculatedEnd(),
                                           granularity));
        URI redirectUri = buildRedirectUri(permissionId, wrapper.calculatedEnd());
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
            String usagePointId
    ) throws PermissionNotFoundException {
        var exists = repository.existsById(permissionId);
        if (!exists) {
            // unknown state / permissionId => not coming / initiated by our frontend
            throw new PermissionNotFoundException(permissionId);
        }
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        if (usagePointId == null) { // probably when request was denied
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
        } else {
            outbox.commit(new FrAcceptedEvent(permissionId, usagePointId));
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
