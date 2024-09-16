package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.ConnectionStatusMessage;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.AccountingPointDataNeed;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.MijnAansluitingApi;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlCreatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlMalformedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlSimpleEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.NlValidatedEvent;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.NL_ZONE_ID;
import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.REGION_CONNECTOR_ID;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final OAuthManager oAuthManager;
    private final Outbox outbox;
    private final DataNeedsService dataNeedService;
    private final NlPermissionRequestRepository permissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> calculationService;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    // The DataNeedsService is autowired from another spring context
    public PermissionRequestService(
            OAuthManager oAuthManager,
            Outbox outbox,
            DataNeedsService dataNeedService,
            NlPermissionRequestRepository permissionRequestRepository,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.oAuthManager = oAuthManager;
        this.outbox = outbox;
        this.dataNeedService = dataNeedService;
        this.permissionRequestRepository = permissionRequestRepository;
        this.calculationService = calculationService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating permission request with id {}", permissionId);
        ZonedDateTime now = ZonedDateTime.now(NL_ZONE_ID);
        var dataNeedId = permissionRequest.dataNeedId();
        outbox.commit(new NlCreatedEvent(permissionId,
                                         permissionRequest.connectionId(),
                                         dataNeedId,
                                         now));
        var calculation = calculationService.calculate(dataNeedId);
        var oauthRequest = switch (calculation) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new NlMalformedEvent(
                        permissionId,
                        List.of(new AttributeError(DATA_NEED_ID, "Unknown dataNeedId"))
                ));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new NlMalformedEvent(permissionId,
                                                   List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) -> {
                var authorizationUrl = oAuthManager.createAuthorizationUrl(permissionRequest.verificationCode(),
                                                                           MijnAansluitingApi.SINGLE_CONSENT_API);
                outbox.commit(new NlValidatedEvent(
                        permissionId,
                        authorizationUrl.state(),
                        authorizationUrl.codeVerifier(),
                        null,
                        permissionTimeframe.start(),
                        permissionTimeframe.end()
                ));
                yield authorizationUrl;
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult -> {
                var authorizationUrl = oAuthManager.createAuthorizationUrl(permissionRequest.verificationCode(),
                                                                           MijnAansluitingApi.CONTINUOUS_CONSENT_API);
                outbox.commit(new NlValidatedEvent(permissionId,
                                                   authorizationUrl.state(),
                                                   authorizationUrl.codeVerifier(),
                                                   vhdResult.granularities().getFirst(),
                                                   vhdResult.energyTimeframe().start(),
                                                   vhdResult.energyTimeframe().end()
                ));
                yield authorizationUrl;
            }
        };
        return new CreatedPermissionRequest(permissionId, oauthRequest.uri());
    }

    public PermissionProcessStatus receiveResponse(
            URI fullUri,
            String permissionId
    ) throws PermissionNotFoundException {
        PermissionProcessStatus status;
        var pr = permissionRequestRepository.findByPermissionId(permissionId);
        if (pr.isEmpty()) {
            LOGGER.info("Got unknown permission request {}", permissionId);
            throw new PermissionNotFoundException(permissionId);
        }
        var dataNeed = dataNeedService.getById(pr.get().dataNeedId());
        try {
            permissionId = oAuthManager.processCallback(fullUri, permissionId, getApiType(dataNeed));
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

    private static MijnAansluitingApi getApiType(DataNeed dataNeed) {
        return switch (dataNeed) {
            case AccountingPointDataNeed ignored -> MijnAansluitingApi.SINGLE_CONSENT_API;
            default -> MijnAansluitingApi.CONTINUOUS_CONSENT_API;
        };
    }
}
