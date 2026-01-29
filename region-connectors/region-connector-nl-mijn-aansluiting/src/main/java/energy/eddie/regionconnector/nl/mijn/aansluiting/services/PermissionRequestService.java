// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.nl.mijn.aansluiting.services;

import com.nimbusds.oauth2.sdk.ParseException;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.regionconnector.nl.mijn.aansluiting.client.CodeboekApiClient;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.nl.mijn.aansluiting.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.nl.mijn.aansluiting.exceptions.NlValidationException;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthManager;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.OAuthRequestPayload;
import energy.eddie.regionconnector.nl.mijn.aansluiting.oauth.exceptions.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.permission.events.*;
import energy.eddie.regionconnector.nl.mijn.aansluiting.persistence.NlPermissionRequestRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
    private final NlPermissionRequestRepository permissionRequestRepository;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final CodeboekApiClient codeboekApiClient;

    public PermissionRequestService(
            OAuthManager oAuthManager,
            Outbox outbox,
            NlPermissionRequestRepository permissionRequestRepository,
            DataNeedCalculationService<DataNeed> calculationService,
            CodeboekApiClient codeboekApiClient
    ) {
        this.oAuthManager = oAuthManager;
        this.outbox = outbox;
        this.permissionRequestRepository = permissionRequestRepository;
        this.calculationService = calculationService;
        this.codeboekApiClient = codeboekApiClient;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequest) throws DataNeedNotFoundException, UnsupportedDataNeedException, NlValidationException {
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
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) ->
                    createAccountingPointDataPermissionRequest(permissionRequest, permissionTimeframe, permissionId);
            case ValidatedHistoricalDataDataNeedResult vhdResult -> {
                var authorizationUrl = oAuthManager.createAuthorizationUrl(permissionRequest.verificationCode());
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

    private OAuthRequestPayload createAccountingPointDataPermissionRequest(
            PermissionRequestForCreation permissionRequest,
            Timeframe permissionTimeframe,
            String permissionId
    ) throws NlValidationException {
        if (permissionRequest.postalCode() == null) {
            var error = new AttributeError("postalCode", "Missing postal code");
            outbox.commit(new NlMalformedEvent(permissionId, List.of(error)));
            throw new NlValidationException(error);
        }
        var houseNumber = permissionRequest.verificationCode();
        var meteringPoints = codeboekApiClient.meteringPoints(permissionRequest.postalCode(), houseNumber)
                                              .filter(res -> !res.getMeteringPoints().isEmpty())
                                              .onErrorComplete(WebClientResponseException.NotFound.class)
                                              .next()
                                              .block();
        if (meteringPoints == null) {
            var error = new AttributeError("postalCode",
                                           "No metering point exists at this postal code and house number");
            outbox.commit(new NlMalformedEvent(permissionId, List.of(error)));
            throw new NlValidationException(error);
        }
        var authorizationUrl = oAuthManager.createAuthorizationUrl(houseNumber);
        outbox.commit(new NlAccountingPointValidatedEvent(
                permissionId,
                authorizationUrl.state(),
                authorizationUrl.codeVerifier(),
                permissionTimeframe.start(),
                permissionTimeframe.end(),
                houseNumber,
                permissionRequest.postalCode()
        ));
        return authorizationUrl;
    }
}
