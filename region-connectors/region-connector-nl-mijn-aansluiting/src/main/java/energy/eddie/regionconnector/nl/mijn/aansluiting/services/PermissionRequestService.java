package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.dataneeds.utils.DataNeedWrapper;
import energy.eddie.dataneeds.utils.TimeframedDataNeedUtils;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthRequestPayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlCreatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlMalformedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlValidatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.*;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String UNSUPPORTED_DATA_NEED_MESSAGE = "This Region Connector only supports Validated Historical Data Data Needs for Gas and Electricity";
    private static final String UNSUPPORTED_GRANULARITY_MESSAGE = "Required granularity not supported.";
    private static final String DATA_NEED_ID = "dataNeedId";
    private final OAuthManager oAuthManager;
    private final Outbox outbox;
    private final DataNeedsService dataNeedService;
    private final NlPermissionRequestRepository permissionRequestRepository;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // The DataNeedsService is autowired from another spring context
    public PermissionRequestService(
            OAuthManager oAuthManager,
            Outbox outbox,
            DataNeedsService dataNeedService,
            NlPermissionRequestRepository permissionRequestRepository
    ) {
        this.oAuthManager = oAuthManager;
        this.outbox = outbox;
        this.dataNeedService = dataNeedService;
        this.permissionRequestRepository = permissionRequestRepository;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        String permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating permission request with id {}", permissionId);
        ZonedDateTime now = ZonedDateTime.now(NL_ZONE_ID);
        outbox.commit(new NlCreatedEvent(permissionId,
                                         permissionRequest.connectionId(),
                                         permissionRequest.dataNeedId(),
                                         now));
        var wrapper = dataNeedService.findById(permissionRequest.dataNeedId());
        if (wrapper.isEmpty()) {
            outbox.commit(new NlMalformedEvent(
                    permissionId,
                    List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
            ));
            throw new DataNeedNotFoundException(permissionRequest.dataNeedId());
        }
        if (!(wrapper.get() instanceof ValidatedHistoricalDataDataNeed dataNeed)
            || !SUPPORTED_ENERGY_TYPES.contains(dataNeed.energyType())) {

            outbox.commit(new NlMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_DATA_NEED_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   wrapper.get().id(),
                                                   UNSUPPORTED_DATA_NEED_MESSAGE);
        }
        Granularity granularity = findGranularity(dataNeed.minGranularity(), dataNeed.maxGranularity());
        if (granularity == null) {
            outbox.commit(new NlMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID,
                                                                          UNSUPPORTED_GRANULARITY_MESSAGE))));
            throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   UNSUPPORTED_GRANULARITY_MESSAGE);
        }

        DataNeedWrapper timeframe;
        try {
            timeframe = TimeframedDataNeedUtils.calculateRelativeStartAndEnd(dataNeed, now.toLocalDate(),
                                                                             MAX_PERIOD_IN_PAST,
                                                                             MAX_PERIOD_IN_FUTURE);
        } catch (UnsupportedDataNeedException e) {
            outbox.commit(new NlMalformedEvent(permissionId,
                                               List.of(new AttributeError(DATA_NEED_ID, e.getMessage()))));
            throw e;
        }
        OAuthRequestPayload oauthRequest = oAuthManager.createAuthorizationUrl(
                permissionRequest.verificationCode());
        outbox.commit(new NlValidatedEvent(permissionId,
                                           oauthRequest.state(),
                                           oauthRequest.codeVerifier(),
                                           granularity,
                                           timeframe.calculatedStart(),
                                           timeframe.calculatedEnd()
        ));
        return new CreatedPermissionRequest(permissionId, oauthRequest.uri());
    }

    @Nullable
    private static Granularity findGranularity(
            Granularity min,
            Granularity max
    ) {
        for (Granularity granularity : SUPPORTED_GRANULARITIES) {
            if (granularity.minutes() >= min.minutes() && granularity.minutes() <= max.minutes()) {
                return granularity;
            }
        }
        return null;
    }

    public PermissionProcessStatus receiveResponse(
            URI fullUri,
            String permissionId
    ) throws PermissionNotFoundException {
        PermissionProcessStatus status;
        try {
            permissionId = oAuthManager.processCallback(fullUri, permissionId);
            LOGGER.info("Permission request {} accepted.", permissionId);
            status = PermissionProcessStatus.ACCEPTED;
        } catch (UserDeniedAuthorizationException e) {
            LOGGER.info("Permission request {} rejected.", permissionId, e);
            status = PermissionProcessStatus.REJECTED;
        } catch (ParseException | OAuthException | IllegalTokenException | InvalidValidationAddressException e) {
            LOGGER.warn("Permission request {} invalid.", permissionId, e);
            status = PermissionProcessStatus.INVALID;
        } catch (JWTSignatureCreationException | OAuthUnavailableException e) {
            LOGGER.warn("Permission request {} could not be sent to permission administrator.", permissionId, e);
            outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.UNABLE_TO_SEND));
            return PermissionProcessStatus.UNABLE_TO_SEND;
        }
        outbox.commit(new NlSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        outbox.commit(new NlSimpleEvent(permissionId, status));
        return status;
    }

    public ConnectionStatusMessage connectionStatusMessage(String permissionId) throws PermissionNotFoundException {
        var permissionRequest
                = permissionRequestRepository.findByPermissionId(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        return new ConnectionStatusMessage(
                permissionRequest.connectionId(),
                permissionId,
                permissionRequest.dataNeedId(),
                permissionRequest.dataSourceInformation(),
                permissionRequest.status()
        );
    }
}
