package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.ConnectionStatusMessage;
import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.events.OutboundPermissionAcceptEvent;
import energy.eddie.aiida.errors.*;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeedFactory;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.QrCodeDto;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;
import java.util.UUID;

import static energy.eddie.aiida.config.AiidaConfiguration.AIIDA_ZONE_ID;
import static energy.eddie.aiida.models.permission.PermissionStatus.*;
import static energy.eddie.api.v0.PermissionProcessStatus.REJECTED;
import static java.util.Objects.requireNonNull;

@Service
public class PermissionService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final PermissionRepository permissionRepository;
    private final Clock clock;
    private final StreamerManager streamerManager;
    private final HandshakeService handshakeService;
    private final PermissionScheduler permissionScheduler;
    private final AuthService authService;
    private final AiidaLocalDataNeedService aiidaLocalDataNeedService;
    private final AiidaEventPublisher aiidaEventPublisher;

    @Autowired
    public PermissionService(
            PermissionRepository permissionRepository,
            Clock clock,
            StreamerManager streamerManager,
            HandshakeService handshakeService,
            PermissionScheduler permissionScheduler,
            AuthService authService,
            AiidaLocalDataNeedService aiidaLocalDataNeedService,
            AiidaEventPublisher aiidaEventPublisher
    ) {
        this.permissionRepository = permissionRepository;
        this.clock = clock;
        this.streamerManager = streamerManager;
        this.handshakeService = handshakeService;
        this.permissionScheduler = permissionScheduler;
        this.authService = authService;
        this.aiidaLocalDataNeedService = aiidaLocalDataNeedService;
        this.aiidaEventPublisher = aiidaEventPublisher;

        streamerManager.terminationRequestsFlux().subscribe(this::terminationRequestReceived);
    }

    /**
     * Revokes the specified permission by updating its status and records the timestamp and persisting the changes. If
     * an error during shutdown of the AiidaStreamer or sending of the {@link ConnectionStatusMessage} occurs, they are
     * logged but not propagated to the caller.
     *
     * @param permissionId The ID of the permission that should be revoked.
     * @return Updated permission object that has been persisted.
     * @throws PermissionNotFoundException        In case no permission with the specified ID can be found.
     * @throws PermissionStateTransitionException In case the permission has a status that makes it not eligible for
     *                                            revocation.
     * @throws UnauthorizedException              If the current user is not the owner of the Permission.
     */
    @Transactional
    public Permission revokePermission(
            UUID permissionId
    ) throws PermissionNotFoundException, PermissionStateTransitionException, UnauthorizedException, InvalidUserException {
        var permission = findById(permissionId);
        LOGGER.info("Got request to revoke permission with id '{}'.", permission.id());
        authService.checkAuthorizationForPermission(permission);

        if (!isEligibleForRevocation(permission))
            throw new PermissionStateTransitionException(permission.id().toString(),
                                                         REVOKED.name(),
                                                         List.of(ACCEPTED.name(),
                                                                 STREAMING_DATA.name(),
                                                                 WAITING_FOR_START.name()),
                                                         permission.status().name());

        permissionScheduler.removePermission(permissionId);


        Instant revocationTime = clock.instant();
        permission.setStatus(REVOKED);
        permission.setRevokeTime(revocationTime);

        var dataNeedId = requireNonNull(permission.dataNeed()).dataNeedId();
        var connectionId = requireNonNull(permission.connectionId());
        var revokedMessage = new ConnectionStatusMessage(connectionId,
                                                         dataNeedId,
                                                         clock.instant(),
                                                         REVOKED,
                                                         permission.id(),
                                                         permission.eddieId());
        streamerManager.stopStreamer(revokedMessage);
        removeInboundDataSourceIfExists(permission);

        return permission;
    }

    /**
     * Saves a new permission in the database and fetches the details of the permission by executing the first part of
     * the handshake with the associated EDDIE framework. If the permission is not fulfillable by this AIIDA instance,
     * e.g. because the requested data is missing or the start date lies in the past, an {@code UNFULFILLABLE} status message is sent to EDDIE, before a
     * {@link PermissionUnfulfillableException} is thrown.
     * <p>
     * After this method, the status of the permission will be either {@code FETCHED_DETAILS} or {@code UNFULFILLABLE}.
     * </p>
     *
     * @param qrCodeDto Data transfer object containing the information for the new permission.
     * @return Permission object with the details as fetched from EDDIE.
     * @throws PermissionAlreadyExistsException If there is already a permission with the ID.
     * @throws PermissionUnfulfillableException If the permission cannot be fulfilled for whatever reason.
     * @throws InvalidUserException             If the id of the current user can not be determined by the token.
     */
    @Transactional
    public Permission setupNewPermission(QrCodeDto qrCodeDto) throws PermissionAlreadyExistsException, PermissionUnfulfillableException, DetailFetchingFailedException, InvalidUserException {
        var currentUserId = authService.getCurrentUserId();
        var permissionId = qrCodeDto.permissionId();

        if (permissionRepository.existsById(permissionId)) {
            throw new PermissionAlreadyExistsException(permissionId);
        }

        var permission = permissionRepository.save(new Permission(qrCodeDto, currentUserId));
        LOGGER.info("Saved new permission ({}) in database, will fetch details from EDDIE framework ({})",
                    permission.id(),
                    permission.eddieId());

        var details = handshakeService.fetchDetailsForPermission(permission)
                                      .doOnError(error -> LOGGER.atError()
                                                                .addArgument(qrCodeDto.permissionId())
                                                                .setCause(error)
                                                                .log("Error while fetching details for permission {} from the EDDIE framework"))
                                      .onErrorComplete()
                                      .block();

        if (details == null) {
            throw new DetailFetchingFailedException(permissionId);
        }

        var now = LocalDate.now(ZoneId.systemDefault());
        if (details.start().isBefore(now)) {
            markPermissionAsUnfulfillable(permission);
        }

        return updatePermissionWithDetails(permission, details);
    }

    /**
     * Reject the specified permission. Updates the database and informs the EDDIE framework of the permission about the
     * rejection.
     *
     * @param permissionId The ID of the permission that should be rejected.
     * @return The updated permission object.
     * @throws PermissionStateTransitionException Thrown if the permission is not in the state
     *                                            {@link PermissionStatus#FETCHED_DETAILS}.
     * @throws UnauthorizedException              If the current user is not the owner of the Permission.
     */
    @Transactional
    public Permission rejectPermission(
            UUID permissionId
    ) throws PermissionStateTransitionException, PermissionNotFoundException, UnauthorizedException, InvalidUserException {
        var permission = findById(permissionId);
        authService.checkAuthorizationForPermission(permission);

        if (permission.status() != FETCHED_DETAILS) {
            throw new PermissionStateTransitionException(permission.id().toString(),
                                                         REJECTED.name(),
                                                         List.of(FETCHED_DETAILS.name()),
                                                         permission.status().name());
        }

        permission.setStatus(PermissionStatus.REJECTED);
        // revokeTime is also used to keep reject timestamp, the status indicates which of the two happened
        permission.setRevokeTime(Instant.now(clock));

        handshakeService.sendUnfulfillableOrRejected(permission, PermissionStatus.REJECTED);

        return permission;
    }

    /**
     * Accept the permission with the passed id. Updates the database and informs the EDDIE framework about the
     * acceptance. Will fetch the MQTT details from the EDDIE framework and either schedules the start (if start date is
     * in the future) or starts the data sharing right away.
     *
     * @param permissionId The ID of the permission that should be accpted.
     * @param dataSourceId The ID of the data source that should be used for the permission.
     * @return The updated permission object.
     * @throws PermissionStateTransitionException Thrown if the permission is not in the state
     *                                            {@link PermissionStatus#FETCHED_DETAILS}.
     * @throws UnauthorizedException              If the current user is not the owner of the Permission.
     */
    @Transactional
    public Permission acceptPermission(
            UUID permissionId,
            @Nullable UUID dataSourceId
    ) throws PermissionStateTransitionException, PermissionNotFoundException, DetailFetchingFailedException, UnauthorizedException, InvalidUserException {
        var permission = findById(permissionId);
        authService.checkAuthorizationForPermission(permission);

        if (permission.status() != FETCHED_DETAILS) {
            throw new PermissionStateTransitionException(permission.id().toString(),
                                                         ACCEPTED.name(),
                                                         List.of(FETCHED_DETAILS.name()),
                                                         permission.status().name());
        }

        permission.setGrantTime(Instant.now(clock));
        permission.setStatus(ACCEPTED);

        if (dataSourceId != null) {
            aiidaEventPublisher.publishEvent(new OutboundPermissionAcceptEvent(permissionId, dataSourceId));
        }

        permission = permissionRepository.save(permission);
        LOGGER.info("Permission ({}) accepted, will fetch MQTT credentials and start or schedule data sharing",
                    permission.id());

        var mqttDto = handshakeService.fetchMqttDetails(permission)
                                      .doOnError(error -> LOGGER.atError()
                                                                .addArgument(permissionId)
                                                                .setCause(error)
                                                                .log("Error while fetching MQTT credentials for permission '{}'"))
                                      .onErrorComplete()
                                      .block();

        if (mqttDto == null) {
            throw new DetailFetchingFailedException(permissionId);
        }

        var mqttStreamingConfig = new MqttStreamingConfig(permissionId, mqttDto);

        permission.setMqttStreamingConfig(mqttStreamingConfig);
        permission.setStatus(FETCHED_MQTT_CREDENTIALS);

        if (permission.dataNeed() instanceof InboundAiidaLocalDataNeed) {
            aiidaEventPublisher.publishEvent(new InboundPermissionAcceptEvent(permission.id()));
        }

        return permissionScheduler.scheduleOrStart(permission);
    }

    /**
     * Returns all permission objects that are persisted, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is
     * the first item.
     */
    public List<Permission> getAllPermissionsSortedByGrantTime() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return permissionRepository.findByUserIdOrderByGrantTimeDesc(currentUserId);
    }

    /**
     * Returns the permission with the specified ID.
     *
     * @param permissionId The unique ID of the permission to be returned.
     * @return The permission object with the specified ID.
     * @throws PermissionNotFoundException In case no permission with the specified ID can be found.
     */
    public Permission findById(UUID permissionId) throws PermissionNotFoundException {
        return permissionRepository.findById(permissionId)
                                   .orElseThrow(() -> new PermissionNotFoundException(permissionId));
    }

    /**
     * Gets all active permissions from the database and checks if they have expired. If not, streaming is resumed,
     * otherwise their database entry will be updated accordingly. This is done when a {@link ContextRefreshedEvent} is
     * received, which ensures that all beans are started and the database is set up correctly.
     */
    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // fields of permissions are loaded eagerly to avoid n+1 select problem in loop
        List<Permission> allActivePermissions = permissionRepository.findAllActivePermissions();
        LOGGER.info(
                "Fetched {} active permissions from database and will resume streaming or update them if they are expired.",
                allActivePermissions.size());

        var now = clock.instant();
        for (Permission permission : allActivePermissions) {
            var expirationTime = permission.expirationTime();
            if (expirationTime == null) {
                LOGGER.error("expirationTime of permission {} is null, this is an unexpected error",
                             permission.id());
                // don't throw exception so other permissions are not skipped
                continue;
            }

            if (expirationTime.isAfter(now)) {
                permissionScheduler.scheduleOrStart(permission);
            } else {
                LOGGER.info("Permission {} has expired but AIIDA was not running at that time, will expire it now",
                            permission.id());
                permission.setStatus(PermissionStatus.FULFILLED);
            }
        }
    }

    @Transactional
    public void addDataSourceToPermission(DataSource dataSource, UUID permissionId) {
        try {
            var permission = findById(permissionId);
            permission.setDataSource(dataSource);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("No permission found with id {}", permissionId, e);
        }
    }

    /**
     * Updates the passed permission with the details from the passed {@link PermissionDetailsDto} object. Also checks
     * if the permission can be fulfilled by this AIIDA instance. Returns the updated object after it has been saved in
     * the database.
     *
     * @throws PermissionUnfulfillableException If the permission cannot be fulfilled by this AIIDA instance.
     * @see PermissionService#isPermissionFulfillable(Permission)
     */
    private Permission updatePermissionWithDetails(
            Permission permission,
            PermissionDetailsDto details
    ) throws PermissionUnfulfillableException {
        var startInstant = ZonedDateTime.of(details.start(), LocalTime.MIN, AIIDA_ZONE_ID).toInstant();
        var endInstant = ZonedDateTime.of(details.end(), LocalTime.MAX.withNano(0), AIIDA_ZONE_ID).toInstant();
        var dataNeedId = details.dataNeed().dataNeedId();

        permission.setConnectionId(details.connectionId());
        permission.setStartTime(startInstant);
        permission.setExpirationTime(endInstant);
        permission.setStatus(FETCHED_DETAILS);

        var aiidaLocalDataNeed = aiidaLocalDataNeedService.optionalAiidaLocalDataNeedById(dataNeedId);
        if (aiidaLocalDataNeed.isPresent()) {
            permission.setDataNeed(aiidaLocalDataNeed.get());
        } else {
            var localDataNeed = AiidaLocalDataNeedFactory.create(details.dataNeed());
            permission.setDataNeed(localDataNeed);
        }

        if (!isPermissionFulfillable(permission)) {
            markPermissionAsUnfulfillable(permission);
        }

        LOGGER.debug("Updated permission {} with details fetched from EDDIE {}",
                     permission.id(),
                     permission.eddieId());
        return permissionRepository.save(permission);
    }

    /**
     * Checks whether the data need associated with {@code permission} can be fulfilled.
     *
     * @return Always true until GH-1040 is properly implemented.
     */
    private boolean isPermissionFulfillable(Permission permission) {
        var dataNeed = permission.dataNeed();
        return dataNeed != null && isValidDataNeedType(dataNeed.type());
    }

    private boolean isValidDataNeedType(String dataNeedType) {
        return dataNeedType.equals(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
               || dataNeedType.equals(InboundAiidaDataNeed.DISCRIMINATOR_VALUE);
    }

    private void terminationRequestReceived(UUID permissionId) {
        LOGGER.info("Will handle termination request for permission '{}'.", permissionId);

        Permission permission;
        try {
            permission = findById(permissionId);
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Was requested to terminate permission '{}', but it cannot be found in the database",
                         permissionId);
            return;
        }

        permissionScheduler.removePermission(permissionId);

        Instant terminateTime = clock.instant();
        permission.setRevokeTime(terminateTime);
        permission.setStatus(TERMINATED);
        permission = permissionRepository.save(permission);

        var dataNeedId = requireNonNull(permission.dataNeed()).dataNeedId();
        var connectionId = requireNonNull(permission.connectionId());
        var terminatedMessage = new ConnectionStatusMessage(connectionId,
                                                            dataNeedId,
                                                            clock.instant(),
                                                            TERMINATED,
                                                            permission.id(),
                                                            permission.eddieId());
        streamerManager.stopStreamer(terminatedMessage);

        removeInboundDataSourceIfExists(permission);
    }

    /**
     * Indicates whether the permission's current status allows it to be revoked. The status needs to be one of the
     * following, to be eligible for revocation: ACCEPTED, WAITING_FOR_START, STREAMING_DATA
     *
     * @param permission Permission to check.
     * @return True if the permission is eligible for revocation, false otherwise.
     */
    private boolean isEligibleForRevocation(Permission permission) {
        return switch (permission.status()) {
            case ACCEPTED, WAITING_FOR_START, STREAMING_DATA -> true;
            default -> false;
        };
    }

    /**
     * Sets the provided {@code permission} as unfulfillable and throws a {@code PermissionUnfulfillableException}
     */
    private void markPermissionAsUnfulfillable(Permission permission) throws PermissionUnfulfillableException {
        permission.setStatus(UNFULFILLABLE);
        permission.setRevokeTime(clock.instant());
        // TODO save reason or at least return to user why it cannot be fulfilled --> GH-1040
        permission = permissionRepository.save(permission);

        handshakeService.sendUnfulfillableOrRejected(permission, UNFULFILLABLE);
        throw new PermissionUnfulfillableException(permission.serviceName());
    }

    private void removeInboundDataSourceIfExists(Permission permission) {
        var dataSource = permission.dataSource();

        if (dataSource != null && dataSource.dataSourceType() == DataSourceType.INBOUND) {
            aiidaEventPublisher.publishEvent(new InboundPermissionRevokeEvent(dataSource.id()));
            permission.setDataSource(null);
        }
    }
}