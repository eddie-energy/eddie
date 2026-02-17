// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.ApplicationInformationAware;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestsDto;
import energy.eddie.api.agnostic.aiida.mqtt.MqttDto;
import energy.eddie.api.agnostic.data.needs.*;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.dtos.PermissionDetailsDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopic;
import energy.eddie.regionconnector.aiida.mqtt.topic.MqttTopicType;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.events.*;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import jakarta.transaction.Transactional;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static energy.eddie.api.v0.PermissionProcessStatus.*;
import static energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID;
import static energy.eddie.regionconnector.aiida.web.PermissionRequestController.PATH_HANDSHAKE_PERMISSION_REQUEST;
import static energy.eddie.regionconnector.shared.utils.CommonPaths.ALL_REGION_CONNECTORS_BASE_URL_PATH;

@Component
public class AiidaPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaPermissionService.class);
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final MqttService mqttService;
    private final AiidaPermissionRequestViewRepository viewRepository;
    private final DataNeedCalculationService<DataNeed> calculationService;
    private final UUID eddieId;
    private final JwtUtil jwtUtil;
    private final String eddiePublicUrl;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // Injected via parent app context
    public AiidaPermissionService(
            Outbox outbox,
            DataNeedsService dataNeedsService,
            MqttService mqttService,
            AiidaPermissionRequestViewRepository viewRepository,
            DataNeedCalculationService<DataNeed> calculationService,
            Sinks.Many<AiidaConnectionStatusMessageDto> statusSink,
            ApplicationContext applicationContext,
            JwtUtil jwtUtil,
            @Value("${eddie.public.url}") String eddiePublicUrl
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
        this.mqttService = mqttService;
        this.viewRepository = viewRepository;
        this.calculationService = calculationService;
        statusSink.asFlux().subscribe(this::statusChangedExternally);
        this.eddieId = applicationContext.getBean(ApplicationInformationAware.BEAN_NAME, UUID.class);
        this.jwtUtil = jwtUtil;
        this.eddiePublicUrl = eddiePublicUrl;
    }

    @Transactional
    public void subscribeToAllActivePermissionTopics() {
        var activePermissions = viewRepository.findActivePermissionRequests();
        LOGGER.info("Found {} active permissions on startup", activePermissions.size());

        for (var permission : activePermissions) {
            subscribeToPermissionTopics(permission.permissionId());
        }
    }

    public void statusChangedExternally(AiidaConnectionStatusMessageDto message) {
        var permissionId = message.permissionId().toString();

        switch (message.status()) {
            case REVOKED -> revokePermission(permissionId);
            case EXTERNALLY_TERMINATED -> externallyTerminatePermission(permissionId);
            case FULFILLED -> fulfillPermission(permissionId);
            default -> LOGGER.warn("Received unsupported external status {} for permission {}",
                                   message.status(),
                                   message.permissionId());
        }
    }

    public AiidaPermissionRequestsDto createValidateAndSendPermissionRequests(
            PermissionRequestForCreation forCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        var permissionIds = new ArrayList<UUID>();
        for (var dataNeedId : forCreation.dataNeedIds()) {
            var permissionId = createValidateAndSendPermissionRequest(forCreation.connectionId(), dataNeedId);
            permissionIds.add(permissionId);
        }

        var accessToken = createAccessToken(permissionIds);

        var handshakeUrlTemplate = eddiePublicUrl
                                   + "/" + ALL_REGION_CONNECTORS_BASE_URL_PATH
                                   + "/" + REGION_CONNECTOR_ID
                                   + PATH_HANDSHAKE_PERMISSION_REQUEST;

        return new AiidaPermissionRequestsDto(eddieId, permissionIds, handshakeUrlTemplate, accessToken);
    }

    public void unableToFulfillPermission(
            String permissionId,
            UUID aiidaId
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, UNFULFILLABLE);

        outbox.commit(new AiidaIdReceivedEvent(permissionId, UNFULFILLABLE, aiidaId));
    }

    public void rejectPermission(
            String permissionId,
            UUID aiidaId
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, REJECTED);

        outbox.commit(new AiidaIdReceivedEvent(permissionId, REJECTED, aiidaId));
    }

    public MqttDto acceptPermission(
            String permissionId,
            UUID aiidaId
    ) throws CredentialsAlreadyExistException, PermissionNotFoundException, PermissionStateTransitionException, DataNeedNotFoundException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, ACCEPTED);

        outbox.commit(new AiidaIdReceivedEvent(permissionId, ACCEPTED, aiidaId));

        var permissionDetails = detailsForPermission(permissionId);
        var dataNeed = permissionDetails.dataNeed();
        var mqttDto = mqttService.createCredentialsAndAclForPermission(permissionId,
                                                                       dataNeed instanceof InboundAiidaDataNeed);
        outbox.commit(new MqttCredentialsCreatedEvent(permissionId));
        subscribeToPermissionTopics(permissionId);

        return mqttDto;
    }

    public void revokePermission(String permissionId) {
        try {
            checkIfPermissionHasValidStatus(permissionId, ACCEPTED, REVOKED);
            mqttService.deleteAclsForPermission(permissionId);
            outbox.commit(new SimpleEvent(permissionId, REVOKED));
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Permission with permission id {} not found", permissionId);
        } catch (PermissionStateTransitionException e) {
            LOGGER.error("Permission with permission id {} could not be revoked", permissionId);
        }
    }

    public void terminatePermission(String permissionId) {
        try {
            AiidaPermissionRequest request = checkIfPermissionHasValidStatus(permissionId, ACCEPTED, TERMINATED);
            mqttService.sendTerminationRequest(request);
            outbox.commit(new SimpleEvent(permissionId, TERMINATED));
            outbox.commit(new SimpleEvent(permissionId, REQUIRES_EXTERNAL_TERMINATION));
        } catch (Exception e) {
            outbox.commit(new FailedToTerminateEvent(permissionId, e.getMessage()));
            LOGGER.warn("Cannot terminate permission {}", permissionId, e);
        }
    }

    public void externallyTerminatePermission(String permissionId) {
        try {
            checkIfPermissionHasValidStatus(permissionId, REQUIRES_EXTERNAL_TERMINATION, EXTERNALLY_TERMINATED);
            mqttService.deleteAclsForPermission(permissionId);
            outbox.commit(new SimpleEvent(permissionId, EXTERNALLY_TERMINATED));
        } catch (Exception e) {
            LOGGER.warn("Cannot externally terminate permission {}", permissionId, e);
        }
    }

    public void fulfillPermission(String permissionId) {
        try {
            checkIfPermissionHasValidStatus(permissionId, ACCEPTED, FULFILLED);
            mqttService.deleteAclsForPermission(permissionId);
            outbox.commit(new SimpleEvent(permissionId, FULFILLED));
        } catch (Exception e) {
            LOGGER.warn("Cannot fulfill permission {}", permissionId, e);
        }
    }

    /**
     * Returns a wrapper containing the {@link AiidaPermissionRequest} and its associated {@link DataNeed}.
     *
     * @throws PermissionNotFoundException If there is no permission with the specified ID saved in the DB.
     * @throws DataNeedNotFoundException   If the data need referenced in the permission request cannot be found in the
     *                                     DB.
     */
    public PermissionDetailsDto detailsForPermission(String permissionId) throws PermissionNotFoundException, DataNeedNotFoundException {
        AiidaPermissionRequest request = viewRepository.findByPermissionId(permissionId)
                                                       .orElseThrow(() -> new PermissionNotFoundException(permissionId));

        DataNeed dataNeed = dataNeedsService.findById(request.dataNeedId())
                                            .orElseThrow(() -> new DataNeedNotFoundException(request.dataNeedId()));

        return new PermissionDetailsDto(eddieId, request, dataNeed);
    }

    private UUID createValidateAndSendPermissionRequest(
            String connectionId,
            String dataNeedId
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException {
        var permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating new permission request with ID {}", permissionId);
        var res = calculationService.calculate(dataNeedId);
        switch (res) {
            case DataNeedNotFoundResult ignored -> throw new DataNeedNotFoundException(dataNeedId);
            case DataNeedNotSupportedResult(String message) -> throw new UnsupportedDataNeedException(
                    AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                    dataNeedId,
                    message);
            case AccountingPointDataNeedResult ignored -> throw new UnsupportedDataNeedException(
                    AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                    dataNeedId,
                    "Data need not supported");
            case ValidatedHistoricalDataDataNeedResult ignored -> throw new UnsupportedDataNeedException(
                    AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                    dataNeedId,
                    "Data need not supported");
            case AiidaDataDataNeedResult aiidaResult -> {
                var terminationTopic = MqttTopic.of(permissionId, MqttTopicType.TERMINATION);
                var createdEvent = new CreatedEvent(permissionId,
                                                    connectionId,
                                                    dataNeedId,
                                                    aiidaResult.energyTimeframe().start(),
                                                    aiidaResult.energyTimeframe().end(),
                                                    terminationTopic.eddieTopic());
                outbox.commit(createdEvent);

                if (validatePermissionRequest(aiidaResult)) {
                    outbox.commit(new SimpleEvent(permissionId, VALIDATED));

                    // we consider displaying the QR code to the user as SENT_TO_PERMISSION_ADMINISTRATOR for AIIDA
                    outbox.commit(new SimpleEvent(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR));
                } else {
                    outbox.commit(new SimpleEvent(permissionId, INVALID));
                }

                return UUID.fromString(permissionId);
            }
        }
    }

    private boolean validatePermissionRequest(AiidaDataDataNeedResult aiidaResult) {
        return aiidaResult.supportsAllSchemas();
    }

    private String createAccessToken(List<UUID> permissionIds) throws JwtCreationFailedException {
        var permissionIdsArray = permissionIds.stream()
                                              .map(UUID::toString)
                                              .toArray(String[]::new);

        return jwtUtil.createJwt(REGION_CONNECTOR_ID, permissionIdsArray);
    }

    private AiidaPermissionRequest checkIfPermissionHasValidStatus(
            String permissionId,
            PermissionProcessStatus requiredStatus,
            PermissionProcessStatus desiredNextStatus
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        Optional<AiidaPermissionRequest> optional = viewRepository.findById(permissionId);
        if (optional.isEmpty()) {
            throw new PermissionNotFoundException(permissionId);
        }
        var request = optional.get();
        LOGGER.atInfo()
              .addArgument(request::permissionId)
              .addArgument(requiredStatus)
              .addArgument(desiredNextStatus)
              .log("Got request check if permission {} is in status {} before transitioning it to status {}");
        // check if in valid previous state because we are reacting to an external event
        if (request.status() != requiredStatus) {
            throw new PermissionStateTransitionException(permissionId,
                                                         desiredNextStatus,
                                                         requiredStatus,
                                                         request.status());
        }

        return request;
    }

    private void subscribeToPermissionTopics(String permissionId) {
        try {
            mqttService.subscribeToOutboundDataTopic(permissionId);
            mqttService.subscribeToStatusTopic(permissionId);
        } catch (MqttException e) {
            LOGGER.error("Something went wrong when subscribing to a topic for permission {}",
                         permissionId,
                         e);
        }
    }
}
