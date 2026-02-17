// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
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
    private final Outbox outbox;
    private final DataNeedCalculationService<DataNeed> calculationService;

    public PermissionRequestService(
            FrPermissionRequestRepository repository,
            EnedisConfiguration configuration,
            Outbox outbox,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.outbox = outbox;
        this.calculationService = calculationService;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        LOGGER.info("Got request to create a new permission, request was: {}", permissionRequestForCreation);
        var permissionId = UUID.randomUUID().toString();

        var result = calculationService.calculate(permissionRequestForCreation.dataNeedId());
        outbox.commit(new FrCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         permissionRequestForCreation.dataNeedId()));
        var end = switch (result) {
            case AiidaDataNeedResult ignored -> {
                String message = "AiidaDataDataNeedResult not supported!";
                outbox.commit(new FrMalformedEvent(permissionId, new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       permissionRequestForCreation.dataNeedId(),
                                                       message);
            }
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new FrMalformedEvent(permissionId,
                                                   new AttributeError(DATA_NEED_ID, "Data need not found")));
                throw new DataNeedNotFoundException(permissionRequestForCreation.dataNeedId());
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new FrMalformedEvent(permissionId,
                                                   new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                       permissionRequestForCreation.dataNeedId(),
                                                       message);
            }
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe) -> {
                handleAccountingPointDataNeed(permissionId, permissionTimeframe);
                yield permissionTimeframe.end();
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult -> {
                handleValidatedHistoricalDataNeed(vhdResult, permissionId);
                yield vhdResult.permissionTimeframe().end();
            }
        };
        var redirectUri = buildRedirectUri(permissionId, end);
        return new CreatedPermissionRequest(permissionId, redirectUri);
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

    public Optional<String> findDataNeedIdForPermission(String permissionId) {
        return repository.findByPermissionId(permissionId).map(PermissionRequest::dataNeedId);
    }

    private void handleAccountingPointDataNeed(String permissionId, Timeframe timeframe) {
        outbox.commit(new FrValidatedEvent(permissionId, timeframe.start(), timeframe.end(), null));
    }

    private void handleValidatedHistoricalDataNeed(
            ValidatedHistoricalDataDataNeedResult calculation,
            String permissionId
    ) {
        outbox.commit(new FrValidatedEvent(permissionId,
                                           calculation.energyTimeframe().start(),
                                           calculation.energyTimeframe().end(),
                                           calculation.granularities().getFirst()));
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
}
