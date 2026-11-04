// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.permission.AcceptPermissionDto;
import energy.eddie.aiida.dtos.permission.PendingPermissionDto;
import energy.eddie.aiida.dtos.permission.RejectPermissionDto;
import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.datasource.IncompatibleDataSourceException;
import energy.eddie.aiida.errors.permission.*;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.*;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeedFactory;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestsDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriTemplate;

import java.time.*;
import java.util.*;

import static energy.eddie.aiida.config.AiidaConfiguration.AIIDA_ZONE_ID;
import static energy.eddie.aiida.models.permission.PermissionStatus.*;
import static java.util.Objects.requireNonNull;

@Service
public class PermissionService implements ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final PermissionRepository permissionRepository;
    private final ConnectionLimitDefaultRepository connectionLimitDefaultRepository;
    private final Clock clock;
    private final StreamerManager streamerManager;
    private final HandshakeService handshakeService;
    private final PermissionScheduler permissionScheduler;
    private final AuthService authService;
    private final DataSourceService dataSourceService;
    private final AiidaLocalDataNeedService aiidaLocalDataNeedService;
    private final AiidaEventPublisher aiidaEventPublisher;
    private final SecretsService secretsService;

    @Autowired
    public PermissionService(
            PermissionRepository permissionRepository,
            ConnectionLimitDefaultRepository connectionLimitDefaultRepository,
            Clock clock,
            StreamerManager streamerManager,
            HandshakeService handshakeService,
            PermissionScheduler permissionScheduler,
            AuthService authService,
            DataSourceService dataSourceService,
            AiidaLocalDataNeedService aiidaLocalDataNeedService,
            AiidaEventPublisher aiidaEventPublisher,
            SecretsService secretsService
    ) {
        this.permissionRepository = permissionRepository;
        this.connectionLimitDefaultRepository = connectionLimitDefaultRepository;
        this.clock = clock;
        this.streamerManager = streamerManager;
        this.handshakeService = handshakeService;
        this.permissionScheduler = permissionScheduler;
        this.authService = authService;
        this.dataSourceService = dataSourceService;
        this.aiidaLocalDataNeedService = aiidaLocalDataNeedService;
        this.aiidaEventPublisher = aiidaEventPublisher;
        this.secretsService = secretsService;
    }

    /**
     * Revokes the specified permission by updating its status and records the timestamp and persisting the changes. If
     * an error during shutdown of the AiidaStreamer or sending of the {@link AiidaConnectionStatusMessageDto} occurs, they are
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
    ) throws PermissionNotFoundException, PermissionStateTransitionException, UnauthorizedException, InvalidUserException, InboundDataSourceInUseException {
        var permission = findById(permissionId);
        LOGGER.info("Got request to revoke permission with id '{}'.", permission.id());
        authService.checkAuthorizationForPermission(permission);

        if (!permission.status().isActive())
            throw new PermissionStateTransitionException(permission.id().toString(),
                                                         REVOKED.name(),
                                                         ACTIVE.stream().map(Enum::name).toList(),
                                                         permission.status().name());

        validateInboundDataSourceNotInUse(permission);
        permissionScheduler.removePermission(permissionId);


        Instant revocationTime = clock.instant();
        permission.setStatus(REVOKED);
        permission.setRevokeTime(revocationTime);

        var dataNeedId = requireNonNull(permission.dataNeed()).dataNeedId();
        var connectionId = requireNonNull(permission.connectionId());
        var revokedMessage = new AiidaConnectionStatusMessageDto(connectionId,
                                                                 dataNeedId,
                                                                 clock.instant(),
                                                                 PermissionProcessStatus.REVOKED,
                                                                 permission.id(),
                                                                 permission.eddieId());
        streamerManager.stopStreamer(revokedMessage);
        removeInboundDataSourceIfExists(permission);

        return permission;
    }

    /**
     * Fetches the details of the permission requests contained in an AIIDA code from the respective EDDIE framework and
     * validates whether this AIIDA instance can fulfill them. Nothing is persisted, so the AIIDA code can be used again
     * as long as the permission requests were neither accepted nor rejected. If a permission request cannot be
     * fulfilled, e.g. because the start date lies in the past, the EDDIE framework is informed with the reason before an
     * exception is thrown.
     *
     * @param permissionRequests Contents of the AIIDA code.
     * @return The details of each permission request.
     * @throws PermissionAlreadyExistsException            There is already an accepted permission with the ID.
     * @throws DetailFetchingFailedException               AIIDA failed the handshake with EDDIE for permission details.
     * @throws InvalidUserException                        The id of the current user can not be determined by the token.
     * @throws ActiveFcaPermissionAlreadyExistsException   There is already an active permission for the meter ID provided in the permission request.
     * @throws PermissionStartInThePastException           The start date lies in the past.
     * @throws PermissionDataNeedTypeNotSupportedException The data need type of the permission is not supported by this AIIDA instance.
     * @throws LimitDefaultsNotAllowedException            Default connection limits were set but the permission is not eligible for them or minLimitKw is not less than maxLimitKw.
     */
    public List<PendingPermissionDto> fetchPermissionDetails(
            AiidaPermissionRequestsDto permissionRequests
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException {
        var currentUserId = authService.getCurrentUserId();

        var pendingPermissions = new ArrayList<PendingPermissionDto>();
        for (var permissionId : permissionRequests.permissionIds()) {
            var permission = buildValidatedPermission(permissionRequests.eddieId(),
                                                      permissionId,
                                                      permissionRequests.handshakeUrl(),
                                                      permissionRequests.accessToken(),
                                                      currentUserId);
            pendingPermissions.add(PendingPermissionDto.from(permission));
        }
        return pendingPermissions;
    }

    /**
     * Accepts the permission request with the passed ID. The details are fetched and validated again, as the client
     * cannot be trusted. Afterward, the permission request is accepted at the EDDIE framework, and the permission is
     * persisted as {@link PermissionStatus#ACCEPTED} together with its MQTT credentials, before data sharing is started
     * or scheduled.
     *
     * @param permissionId The ID of the permission request that should be accepted.
     * @param acceptDto    Handshake information from the AIIDA code and the selections of the user.
     * @return The accepted and persisted permission.
     * @throws MissingDataSourceException     The permission is outbound, but no data source was passed.
     * @throws DataSourceNotFoundException    The data source does not exist or belongs to another user.
     * @throws IncompatibleDataSourceException The data source does not match the asset or schemas of the data need.
     * @throws DetailFetchingFailedException  Fetching the details or accepting at the EDDIE framework failed.
     */
    @Transactional
    public Permission acceptPermission(
            UUID permissionId,
            AcceptPermissionDto acceptDto
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException, MissingDataSourceException, DataSourceNotFoundException, IncompatibleDataSourceException, InvalidInboundPermissionException, SecretStoringException {
        var currentUserId = authService.getCurrentUserId();
        var permission = buildValidatedPermission(acceptDto.eddieId(),
                                                  permissionId,
                                                  acceptDto.handshakeUrl(),
                                                  acceptDto.accessToken(),
                                                  currentUserId);
        var isInbound = permission.dataNeed() instanceof InboundAiidaLocalDataNeed;

        // validate everything before accepting at EDDIE, to keep the window small in which EDDIE considers the
        // permission accepted, but AIIDA fails to persist it
        if (!isInbound) {
            var dataSourceId = acceptDto.dataSourceId();
            if (dataSourceId == null) {
                throw new MissingDataSourceException(permissionId);
            }
            permission.setDataSource(validateDataSourceSelection(permission, dataSourceId));
        }

        var inboundMessageFormat = acceptDto.inboundMessageFormat();
        if (inboundMessageFormat != null) {
            permission.updateInboundMessageFormat(inboundMessageFormat);
        }

        var displayName = acceptDto.displayName();
        if (displayName != null && !displayName.isBlank()) {
            permission.setDisplayName(displayName);
        }

        var mqttDto = handshakeService.accept(HandshakeTarget.of(permission))
                                      .doOnError(error -> LOGGER.atError()
                                                                .addArgument(permissionId)
                                                                .setCause(error)
                                                                .log("Error while accepting permission '{}' at the EDDIE framework"))
                                      .onErrorComplete()
                                      .block();
        if (mqttDto == null) {
            throw new DetailFetchingFailedException(permissionId);
        }

        var mqttStreamingConfig = new MqttStreamingConfig(mqttDto);
        secretsService.storeSecret(mqttStreamingConfig.permissionId(), SecretType.PASSWORD, mqttDto.password());
        permission.setGrantTime(Instant.now(clock));

        // The MQTT streaming config references the permission, but is mapped via @PrimaryKeyJoinColumn, which makes
        // Hibernate insert it before the permission. Attaching it to the already persisted permission orders the
        // inserts correctly.
        permission = permissionRepository.save(permission);
        permission.setMqttStreamingConfig(mqttStreamingConfig);
        persistLimitDefaults(permission);
        LOGGER.info("Permission ({}) accepted, will start or schedule data sharing", permission.id());

        if (isInbound) {
            aiidaEventPublisher.publishEvent(new InboundPermissionAcceptEvent(permission.id()));
        }

        permissionScheduler.scheduleOrStart(ActivePermission.from(permission));
        return permission;
    }

    /**
     * Rejects the permission request with the passed ID at the respective EDDIE framework. Nothing is persisted.
     *
     * @param permissionId The ID of the permission request that should be rejected.
     * @param rejectDto    Handshake information from the AIIDA code and an optional reason.
     * @throws PermissionAlreadyExistsException The permission was already accepted.
     */
    public void rejectPermission(
            UUID permissionId,
            RejectPermissionDto rejectDto
    ) throws PermissionAlreadyExistsException {
        if (permissionRepository.existsById(permissionId)) {
            throw new PermissionAlreadyExistsException(permissionId);
        }

        LOGGER.info("Rejecting permission request {}", permissionId);
        handshakeService.reject(handshakeTarget(rejectDto.handshakeUrl(), permissionId, rejectDto.accessToken()),
                                rejectDto.reason());
    }

    @Transactional
    public Permission updateInboundMessageFormat(
            UUID permissionId,
            @Nullable InboundMessageFormat inboundMessageFormat
    ) throws PermissionNotFoundException, UnauthorizedException, InvalidUserException,
             MissingInboundMessageFormatException, InvalidInboundPermissionException {
        if (inboundMessageFormat == null) {
            throw new MissingInboundMessageFormatException();
        }

        var permission = findById(permissionId);
        authService.checkAuthorizationForPermission(permission);

        var previousInboundMessageFormat = permission.inboundMessageFormat();
        permission.updateInboundMessageFormat(inboundMessageFormat);

        LOGGER.info("Updated inbound message format for permission {} from {} to {}.",
                    permission.id(),
                    previousInboundMessageFormat,
                    inboundMessageFormat);

        return permission;
    }

    /**
     * Updates the display name of the permission with the passed id.
     *
     * @param permissionId The ID of the permission that should be renamed.
     * @param displayName  The new display name for the permission.
     * @return The updated permission object.
     * @throws UnauthorizedException        If the current user is not the owner of the Permission.
     * @throws MissingDisplayNameException  If displayName is null or blank.
     */
    @Transactional
    public Permission updateDisplayName(
            UUID permissionId,
            @Nullable String displayName
    ) throws PermissionNotFoundException, UnauthorizedException, InvalidUserException, MissingDisplayNameException {
        if (displayName == null || displayName.isBlank()) {
            throw new MissingDisplayNameException();
        }

        var permission = findById(permissionId);
        authService.checkAuthorizationForPermission(permission);

        permission.setDisplayName(displayName);

        LOGGER.info("Updated display name for permission {}.", permission.id());

        return permission;
    }

    /**
     * Returns all permission objects that are persisted, sorted by their grantTime descending.
     *
     * @return A list of permissions, sorted by grantTime descending, i.e. the permission with the newest grantTime is
     * the first item.
     */
    public List<Permission> getAllPermissionsSortedByGrantTime() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();

        return permissionRepository.findByUserIdOrderByGrantTimeDescRevokeTimeDesc(currentUserId);
    }

    public List<Permission> getActiveInboundPermissions() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();
        return permissionRepository.findInboundByUserIdAndStatus(currentUserId, PermissionStatus.ACTIVE);
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
     * Gets all accepted permissions with MQTT credentials from the database and checks if they have expired.
     * If not, streaming is resumed, otherwise their database entry will be updated accordingly.
     * This is done when a {@link ContextRefreshedEvent} is received,
     * which ensures that all beans are started and the database is set up correctly.
     */
    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ContextRefreshedEvent event) {
        // fields of permissions are loaded eagerly to avoid n+1 select problem in loop
        var acceptedPermissions = permissionRepository.findStartableByStatus(ACCEPTED);
        LOGGER.info(
                "Fetched {} permission from database that should resume streaming or be updated if expired.",
                acceptedPermissions.size());

        var now = clock.instant();
        for (Permission permission : acceptedPermissions) {
            var expirationTime = permission.expirationTime();
            if (expirationTime == null) {
                LOGGER.error("expirationTime of permission {} is null, this is an unexpected error",
                             permission.id());
                // don't throw exception so other permissions are not skipped
                continue;
            }

            if (!expirationTime.isAfter(now)) {
                LOGGER.info("Permission {} has expired but AIIDA was not running at that time, will expire it now",
                            permission.id());
                permission.setStatus(PermissionStatus.FULFILLED);
                continue;
            }

            try {
                permissionScheduler.scheduleOrStart(ActivePermission.from(permission));
            } catch (IllegalStateException exception) {
                // don't throw exception so other permissions are not skipped
                LOGGER.error("Cannot resume streaming for permission {}", permission.id(), exception);
            }
        }
    }

    /**
     * Builds a permission from the details fetched from the EDDIE framework and validates whether this AIIDA instance
     * can fulfill it.
     *
     * @see PermissionService#validatePermissionStartInTheFuture(Permission)
     * @see PermissionService#validateDataNeedType(Permission)
     * @see PermissionService#validateLimitDefaults(Permission)
     * @see PermissionService#validateSingleActiveFcaPermissionPerMeterId(Permission)
     */
    private Permission buildValidatedPermission(
            UUID eddieId,
            UUID permissionId,
            String handshakeUrlTemplate,
            String accessToken,
            UUID currentUserId
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException {
        if (permissionRepository.existsById(permissionId)) {
            throw new PermissionAlreadyExistsException(permissionId);
        }

        var target = handshakeTarget(handshakeUrlTemplate, permissionId, accessToken);
        var details = handshakeService.fetchDetails(target)
                                      .doOnError(error -> LOGGER.atError()
                                                                .addArgument(permissionId)
                                                                .setCause(error)
                                                                .log("Error while fetching details for permission {} from the EDDIE framework"))
                                      .onErrorComplete()
                                      .block();
        if (details == null) {
            throw new DetailFetchingFailedException(permissionId);
        }

        var permission = new Permission(eddieId, permissionId, target.handshakeUrl(), accessToken, currentUserId);
        permission.setConnectionId(details.connectionId());
        permission.setMeterId(details.meterId());
        permission.setStartTime(ZonedDateTime.of(details.start(), LocalTime.MIN, AIIDA_ZONE_ID).toInstant());
        permission.setExpirationTime(ZonedDateTime.of(details.end(), LocalTime.MAX.withNano(0), AIIDA_ZONE_ID)
                                                  .toInstant());
        // Limits are only persisted as connection limit defaults once the permission is accepted
        permission.setMinLimitKw(details.minLimitKw());
        permission.setMaxLimitKw(details.maxLimitKw());

        var dataNeedId = details.dataNeed().dataNeedId();
        var localDataNeed = aiidaLocalDataNeedService.optionalAiidaLocalDataNeedById(dataNeedId)
                                                     .orElseGet(() -> AiidaLocalDataNeedFactory.create(details.dataNeed()));
        permission.initializeFromDataNeed(localDataNeed);

        validatePermissionStartInTheFuture(permission);
        validateDataNeedType(permission);
        validateLimitDefaults(permission);
        validateSingleActiveFcaPermissionPerMeterId(permission);

        return permission;
    }

    private static HandshakeTarget handshakeTarget(String handshakeUrlTemplate, UUID permissionId, String accessToken) {
        var handshakeUrl = new UriTemplate(handshakeUrlTemplate).expand(permissionId).toString();
        return new HandshakeTarget(handshakeUrl, accessToken);
    }

    private void persistLimitDefaults(Permission permission) {
        if (permission.minLimitKw() != null || permission.maxLimitKw() != null) {
            var defaultLimit = new ConnectionLimitDefault(permission.id(),
                                                          permission.meterId(),
                                                          clock.instant(),
                                                          null,
                                                          permission.minLimitKw(),
                                                          permission.maxLimitKw());
            connectionLimitDefaultRepository.save(defaultLimit);
        }
    }

    private void validatePermissionStartInTheFuture(Permission permission) throws PermissionStartInThePastException {
        var start = requireNonNull(permission.startTime()).atZone(AIIDA_ZONE_ID);
        var today = LocalDate.now(AIIDA_ZONE_ID).atStartOfDay(AIIDA_ZONE_ID);
        if (start.isBefore(today)) {
            var exception = new PermissionStartInThePastException(permission.id(), start);
            denotePermissionAsUnfulfillable(permission, exception.getMessage());
            throw exception;
        }
    }

    private void validateDataNeedType(Permission permission) throws PermissionDataNeedTypeNotSupportedException {
        var dataNeedType = requireNonNull(permission.dataNeed()).type();
        if (dataNeedType == null || !isValidDataNeedType(dataNeedType)) {
            var exception = new PermissionDataNeedTypeNotSupportedException(permission.id(), dataNeedType);
            denotePermissionAsUnfulfillable(permission, exception.getMessage());
            throw exception;
        }
    }

    private void validateLimitDefaults(Permission permission) throws LimitDefaultsNotAllowedException {
        var minLimitKw = permission.minLimitKw();
        var maxLimitKw = permission.maxLimitKw();
        if (minLimitKw == null && maxLimitKw == null) {
            return;
        }

        var isRangeValid = minLimitKw == null || maxLimitKw == null || minLimitKw.compareTo(maxLimitKw) < 0;
        var dataNeed = Objects.requireNonNull(permission.dataNeed());
        if (!dataNeed.supportsConnectionLimits() || !isRangeValid) {
            var exception = new LimitDefaultsNotAllowedException(permission.id());
            denotePermissionAsUnfulfillable(permission, exception.getMessage());
            throw exception;
        }
    }

    private boolean isValidDataNeedType(String dataNeedType) {
        return OutboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeedType)
               || InboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeedType);
    }

    private void denotePermissionAsUnfulfillable(Permission permission, @Nullable String reason) {
        LOGGER.info("Permission request {} cannot be fulfilled: {}", permission.id(), reason);
        handshakeService.denoteUnfulfillable(HandshakeTarget.of(permission), reason);
    }

    private void validateSingleActiveFcaPermissionPerMeterId(Permission permission) throws ActiveFcaPermissionAlreadyExistsException {
        var dataNeed = permission.dataNeed();
        if (dataNeed == null || !dataNeed.contexts().contains(AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT)) {
            return;
        }

        var meterId = permission.meterId();
        if (meterId == null || meterId.isBlank()) {
            return;
        }

        var hasConflict = permissionRepository.existsByUserIdAndDataNeedTypeAndContextAndMeterIdAndStatusIn(requireNonNull(permission.userId()),
                                                                                                            dataNeed.type(),
                                                                                                            AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT,
                                                                                                            meterId,
                                                                                                            PermissionStatus.ACTIVE);

        if (hasConflict) {
            var exception = new ActiveFcaPermissionAlreadyExistsException(permission.id(), meterId, dataNeed.type());
            denotePermissionAsUnfulfillable(permission, exception.getMessage());
            throw exception;
        }
    }

    private DataSource validateDataSourceSelection(Permission permission, UUID dataSourceId)
            throws DataSourceNotFoundException, IncompatibleDataSourceException {
        var dataSource = dataSourceService.dataSourceByIdOrThrow(dataSourceId);

        if (!Objects.equals(dataSource.userId(), permission.userId())) {
            throw new DataSourceNotFoundException(dataSourceId);
        }

        var requiredAsset = requireNonNull(permission.dataNeed()).asset();
        if (dataSource.asset() != requiredAsset) {
            throw new IncompatibleDataSourceException(
                    dataSourceId,
                    "The selected data source has asset '%s', but the data need requires '%s'.".formatted(
                            dataSource.asset(),
                            requiredAsset)
            );
        }

        if (dataSource instanceof InboundDataSource inboundDataSource) {
            var requiredSchemas = requireNonNull(permission.dataNeed()).schemas();
            if (!hasSchemaOverlap(requiredSchemas, inboundDataSource.schemas())) {
                throw new IncompatibleDataSourceException(
                        dataSourceId,
                        "The selected inbound data source does not match any requested schema."
                );
            }
        }
        return dataSource;
    }

    private boolean hasSchemaOverlap(Set<AiidaSchema> requiredSchemas, Set<AiidaSchema> availableSchemas) {
        return requiredSchemas.stream().anyMatch(availableSchemas::contains);
    }

    private void removeInboundDataSourceIfExists(Permission permission) {
        var dataSourceId = inboundDataSourceIdIfInboundPermission(permission);
        if (dataSourceId != null) {
            aiidaEventPublisher.publishEvent(new InboundPermissionRevokeEvent(dataSourceId));
            permission.setDataSource(null);
        }
    }

    private void validateInboundDataSourceNotInUse(Permission permission) throws InboundDataSourceInUseException {
        var dataSourceId = inboundDataSourceIdIfInboundPermission(permission);
        if (dataSourceId != null) {
            var permissions = permissionRepository.findOutboundByDataSourceIdAndStatus(dataSourceId, ACTIVE);
            if (!permissions.isEmpty()) {
                var blockingIds = permissions.stream().map(Permission::id).toList();
                LOGGER.trace(
                        "Revoke on inbound permission {} was blocked because its data source {} is used by outbound permissions {}.",
                        permission.id(),
                        dataSourceId,
                        blockingIds);
                throw new InboundDataSourceInUseException(permission.id(), blockingIds);
            }
        }
    }

    private @Nullable UUID inboundDataSourceIdIfInboundPermission(Permission permission) {
        return permission.dataSource() instanceof InboundDataSource inboundDataSource &&
               permission.equals(inboundDataSource.permission()) ? inboundDataSource.id() : null;
    }
}