// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.fr.enedis.services;

import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionRequest;
import energy.eddie.api.agnostic.process.model.validation.AttributeError;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.regionconnector.fr.enedis.api.EnedisSubscribedServicesApi;
import energy.eddie.regionconnector.fr.enedis.config.EnedisConfiguration;
import energy.eddie.regionconnector.fr.enedis.dto.Authorization;
import energy.eddie.regionconnector.fr.enedis.dto.subscription.ServiceState;
import energy.eddie.regionconnector.fr.enedis.dto.subscription.SubscribedService;
import energy.eddie.regionconnector.fr.enedis.permission.events.*;
import energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.CreatedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.fr.enedis.persistence.FrPermissionRequestRepository;
import energy.eddie.regionconnector.fr.enedis.utils.EnedisDuration;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import org.apache.http.client.utils.URIBuilder;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.fr.enedis.EnedisRegionConnectorMetadata.ZONE_ID_FR;

@Service
public class PermissionRequestService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionRequestService.class);
    private static final String DATA_NEED_ID = "dataNeedId";
    private final FrPermissionRequestRepository repository;
    private final EnedisConfiguration configuration;
    private final Outbox outbox;
    private final DataNeedCalculationService calculationService;
    private final EnedisSubscribedServicesApi enedisApiClient;

    public PermissionRequestService(
            FrPermissionRequestRepository repository,
            EnedisConfiguration configuration,
            Outbox outbox,
            DataNeedCalculationService calculationService,
            EnedisSubscribedServicesApi enedisApiClient
    ) {
        this.repository = repository;
        this.configuration = configuration;
        this.outbox = outbox;
        this.calculationService = calculationService;
        this.enedisApiClient = enedisApiClient;
    }

    public CreatedPermissionRequest createPermissionRequest(PermissionRequestForCreation permissionRequestForCreation) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        LOGGER.info("Got request to create a new permission, request was: {} with permission ID {}",
                    permissionRequestForCreation,
                    permissionId);

        var dataNeedId = permissionRequestForCreation.dataNeedId();
        var result = calculationService.calculate(dataNeedId);
        outbox.commit(new FrCreatedEvent(permissionId,
                                         permissionRequestForCreation.connectionId(),
                                         dataNeedId));
        var end = switch (result) {
            case DataNeedNotFoundResult ignored -> {
                outbox.commit(new FrMalformedEvent(permissionId,
                                                   new AttributeError(DATA_NEED_ID, "Data need not found")));
                throw new DataNeedNotFoundException(dataNeedId);
            }
            case DataNeedNotSupportedResult(String message) -> {
                outbox.commit(new FrMalformedEvent(permissionId,
                                                   new AttributeError(DATA_NEED_ID, message)));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID,
                                                       dataNeedId,
                                                       message);
            }
            case AccountingPointDataNeedResult(Timeframe permissionTimeframe, var ignored) -> {
                handleAccountingPointDataNeed(permissionId, permissionTimeframe);
                yield permissionTimeframe.end();
            }
            case ValidatedHistoricalDataDataNeedResult vhdResult -> {
                handleValidatedHistoricalDataNeed(vhdResult, permissionId);
                yield vhdResult.permissionTimeframe().end();
            }
            default -> {
                var message = "Data Need not supported";
                outbox.commit(new FrMalformedEvent(permissionId, List.of(new AttributeError(DATA_NEED_ID, message))));
                throw new UnsupportedDataNeedException(REGION_CONNECTOR_ID, dataNeedId, message);
            }
        };
        var redirectUri = buildRedirectUri(permissionId, end);
        return new CreatedPermissionRequest(permissionId, redirectUri);
    }

    public Authorization authorizePermissionRequest(
            String permissionId,
            @Nullable Long authorizationId
    ) throws PermissionNotFoundException {
        LOGGER.info("Got request to authorize a permission with permission ID {}", permissionId);
        var permissionRequest = repository
                .findByPermissionId(permissionId)
                .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
        if (authorizationId == null) {
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.REJECTED));
            LOGGER.info("Permission request {} was rejected, since no authorization id was provided", permissionId);
            return Authorization.REJECTED;
        }

        var services = enedisApiClient.getSubscribedServices(authorizationId)
                                      .map(response -> response.services() == null
                                              ? List.<SubscribedService>of()
                                              : response.services())
                                      .onErrorReturn(List.of())
                                      .block();
        var usagePointIds = activeUsagePointIds(services);
        if (usagePointIds.isEmpty()) {
            LOGGER.warn("No usage point id found for authorization id '{}'", authorizationId);
            outbox.commit(new FrSimpleEvent(permissionId, PermissionProcessStatus.INVALID));
            return Authorization.INVALID;
        }

        // Enedis reuses one authorization for all consents of a customer, so the services contain the usage points of
        // earlier consents too, which already have a permission request.
        var acceptedUsagePointIds = repository.findAllByConnectionIdAndDataNeedIdAndStatus(
                                                      permissionRequest.connectionId(),
                                                      permissionRequest.dataNeedId(),
                                                      PermissionProcessStatus.ACCEPTED
                                              )
                                              .stream()
                                              .map(EnedisPermissionRequest::usagePointId)
                                              .filter(Objects::nonNull)
                                              .collect(Collectors.toSet());
        var newUsagePointIds = usagePointIds.stream()
                                            .filter(id -> !acceptedUsagePointIds.contains(id))
                                            .toList();
        var usagePointId = newUsagePointIds.isEmpty() ? usagePointIds.getFirst() : newUsagePointIds.getFirst();
        var granularity = permissionRequest.granularity();
        outbox.commit(new FrAcceptedEvent(permissionId, usagePointId, granularity));

        for (var additionalUsagePointId : newUsagePointIds) {
            if (additionalUsagePointId.equals(usagePointId)) {
                continue;
            }
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
                    granularity
            ));
            outbox.commit(new FrSimpleEvent(newPermissionId, PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR));
            outbox.commit(new FrAcceptedEvent(newPermissionId, additionalUsagePointId, granularity));
        }
        return Authorization.ACCEPTED;
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
                    .setPath("/dataconnect/v2/oauth2/authorize")
                    .addParameter("client_id", configuration.clientId())
                    .addParameter("response_type", "code")
                    .addParameter("state", permissionId)
                    .addParameter("duration", new EnedisDuration(end).toString())
                    .build();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Unable to create redirect URI");
        }
    }

    /**
     * Returns the distinct usage point ids of the services that are active today, the usage points of the most recently
     * started services first. Enedis returns one service per measure type and direction, so a usage point is usually
     * part of several services.
     */
    private static List<String> activeUsagePointIds(@Nullable List<SubscribedService> services) {
        if (services == null) {
            return List.of();
        }
        var today = LocalDate.now(ZONE_ID_FR);
        return services.stream()
                       .filter(service -> service.pointId() != null)
                       .filter(service -> service.state() == null || service.state() == ServiceState.ACTIVE)
                       .filter(service -> service.end() == null || !service.end().isBefore(today))
                       .sorted(Comparator.comparing(SubscribedService::start,
                                                    Comparator.nullsLast(Comparator.reverseOrder())))
                       .map(SubscribedService::pointId)
                       .distinct()
                       .toList();
    }
}
