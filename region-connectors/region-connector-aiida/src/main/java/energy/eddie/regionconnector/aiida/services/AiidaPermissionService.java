package energy.eddie.regionconnector.aiida.services;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.dataneeds.exceptions.DataNeedNotFoundException;
import energy.eddie.dataneeds.exceptions.UnsupportedDataNeedException;
import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import energy.eddie.regionconnector.aiida.config.AiidaConfiguration;
import energy.eddie.regionconnector.aiida.dtos.PermissionDetailsDto;
import energy.eddie.regionconnector.aiida.dtos.PermissionRequestForCreation;
import energy.eddie.regionconnector.aiida.dtos.QrCodeDto;
import energy.eddie.regionconnector.aiida.exceptions.CredentialsAlreadyExistException;
import energy.eddie.regionconnector.aiida.mqtt.MqttDto;
import energy.eddie.regionconnector.aiida.mqtt.MqttService;
import energy.eddie.regionconnector.aiida.permission.request.AiidaPermissionRequest;
import energy.eddie.regionconnector.aiida.permission.request.events.CreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.FailedToTerminateEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.MqttCredentialsCreatedEvent;
import energy.eddie.regionconnector.aiida.permission.request.events.SimpleEvent;
import energy.eddie.regionconnector.aiida.permission.request.persistence.AiidaPermissionRequestViewRepository;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import energy.eddie.regionconnector.shared.exceptions.JwtCreationFailedException;
import energy.eddie.regionconnector.shared.exceptions.PermissionNotFoundException;
import energy.eddie.regionconnector.shared.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriTemplate;

import java.util.Optional;
import java.util.UUID;

import static energy.eddie.api.v0.PermissionProcessStatus.*;

@Component
public class AiidaPermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiidaPermissionService.class);
    private final Outbox outbox;
    private final DataNeedsService dataNeedsService;
    private final AiidaConfiguration configuration;
    private final MqttService mqttService;
    private final AiidaPermissionRequestViewRepository viewRepository;
    private final JwtUtil jwtUtil;
    private final DataNeedCalculationService<DataNeed> calculationService;

    public AiidaPermissionService(
            Outbox outbox,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")  // defined in core
            DataNeedsService dataNeedsService,
            AiidaConfiguration configuration,
            MqttService mqttService,
            AiidaPermissionRequestViewRepository viewRepository,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") // is injected from another Spring context
            JwtUtil jwtUtil,
            DataNeedCalculationService<DataNeed> calculationService
    ) {
        this.outbox = outbox;
        this.dataNeedsService = dataNeedsService;
        this.configuration = configuration;
        this.mqttService = mqttService;
        this.viewRepository = viewRepository;
        this.jwtUtil = jwtUtil;
        this.calculationService = calculationService;
    }

    public QrCodeDto createValidateAndSendPermissionRequest(
            PermissionRequestForCreation forCreation
    ) throws DataNeedNotFoundException, UnsupportedDataNeedException, JwtCreationFailedException {
        var permissionId = UUID.randomUUID().toString();
        LOGGER.info("Creating new permission request with ID {}", permissionId);

        var dataNeed = dataNeedsService.findById(forCreation.dataNeedId())
                                       .orElseThrow(() -> new DataNeedNotFoundException(forCreation.dataNeedId()));

        var calculation = calculationService.calculate(dataNeed);
        if (!calculation.supportsDataNeed() || calculation.energyDataTimeframe() == null) {
            throw new UnsupportedDataNeedException(AiidaRegionConnectorMetadata.REGION_CONNECTOR_ID,
                                                   dataNeed.id(),
                                                   "Unsupported data need");
        }
        var terminationTopic = terminationTopicForPermissionId(permissionId);
        var createdEvent = new CreatedEvent(permissionId,
                                            forCreation.connectionId(),
                                            forCreation.dataNeedId(),
                                            calculation.energyDataTimeframe().start(),
                                            calculation.energyDataTimeframe().end(),
                                            terminationTopic);

        outbox.commit(createdEvent);
        // no validation for AIIDA requests necessary
        outbox.commit(new SimpleEvent(permissionId, VALIDATED));
        // we consider displaying the QR code to the user as SENT_TO_PERMISSION_ADMINISTRATOR for AIIDA
        outbox.commit(new SimpleEvent(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR));

        var handshakeUrl = new UriTemplate(configuration.handshakeUrl()).expand(permissionId).toString();
        var jwtString = jwtUtil.createAiidaJwt(permissionId);

        return new QrCodeDto(permissionId, dataNeed.name(), handshakeUrl, jwtString);
    }

    private String terminationTopicForPermissionId(String permissionId) {
        return "aiida/v1/" + permissionId + "/termination";
    }

    public MqttDto acceptPermission(String permissionId) throws CredentialsAlreadyExistException, PermissionNotFoundException, PermissionStateTransitionException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, ACCEPTED);

        outbox.commit(new SimpleEvent(permissionId, ACCEPTED));

        var mqttDto = mqttService.createCredentialsAndAclForPermission(permissionId);

        outbox.commit(new MqttCredentialsCreatedEvent(permissionId, mqttDto.username()));

        return mqttDto;
    }

    private AiidaPermissionRequest checkIfPermissionHasValidStatus(
            String permissionId,
            PermissionProcessStatus requiredStatus,
            PermissionProcessStatus desiredNextStatus
    ) throws PermissionNotFoundException, PermissionStateTransitionException {
        Optional<AiidaPermissionRequest> optional = viewRepository.findById(permissionId);
        if (optional.isEmpty()) {
            LOGGER.warn(
                    "Got request check if permission {} is in status {} before transitioning it to status {}, but there is no permission with this ID in the database",
                    permissionId,
                    requiredStatus,
                    desiredNextStatus);
            throw new PermissionNotFoundException(permissionId);
        }
        var request = optional.get();

        // check if in valid previous state because we are reacting to an external event
        if (request.status() != requiredStatus) {
            throw new PermissionStateTransitionException(
                    permissionId,
                    desiredNextStatus,
                    requiredStatus,
                    request.status());
        }

        return request;
    }

    public void unableToFulFillPermission(String permissionId) throws PermissionNotFoundException, PermissionStateTransitionException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, UNFULFILLABLE);

        outbox.commit(new SimpleEvent(permissionId, UNFULFILLABLE));
    }

    public void rejectPermission(String permissionId) throws PermissionNotFoundException, PermissionStateTransitionException {
        checkIfPermissionHasValidStatus(permissionId, SENT_TO_PERMISSION_ADMINISTRATOR, REJECTED);

        outbox.commit(new SimpleEvent(permissionId, REJECTED));
    }

    public void terminatePermission(String permissionId) {
        LOGGER.info("Got request to terminate permission {}", permissionId);

        // TODO How to handle ACLs for both events? Delete immediately? Schedule task? --> GH-970
        try {
            AiidaPermissionRequest request = checkIfPermissionHasValidStatus(permissionId, ACCEPTED, TERMINATED);
            mqttService.sendTerminationRequest(request);
            outbox.commit(new SimpleEvent(permissionId, TERMINATED));
        } catch (Exception e) {
            outbox.commit(new FailedToTerminateEvent(permissionId, e.getMessage()));
            LOGGER.warn("Cannot terminate permission {}", permissionId, e);
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
                                                       .orElseThrow(() -> new PermissionNotFoundException(
                                                               permissionId));

        DataNeed dataNeed = dataNeedsService.findById(request.dataNeedId())
                                            .orElseThrow(() -> new DataNeedNotFoundException(request.dataNeedId()));

        return new PermissionDetailsDto(request, dataNeed);
    }
}
