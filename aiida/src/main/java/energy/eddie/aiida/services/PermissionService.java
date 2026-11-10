// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.permission.*;
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
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeedFactory;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.InboundPermissionRepository;
import energy.eddie.aiida.repositories.OutboundPermissionRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.services.secrets.SecretType;
import energy.eddie.aiida.services.secrets.SecretsService;
import energy.eddie.api.agnostic.aiida.AiidaContext;
import energy.eddie.api.agnostic.aiida.AiidaPermissionRequestsDto;
import energy.eddie.api.agnostic.aiida.AiidaSchema;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

import static energy.eddie.aiida.config.AiidaConfiguration.AIIDA_ZONE_ID;
import static energy.eddie.aiida.models.permission.PermissionStatus.ACCEPTED;
import static energy.eddie.aiida.models.permission.PermissionStatus.REVOKED;

@Service
public class PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final PermissionRepository permissionRepository;
    private final InboundPermissionRepository inboundPermissionRepository;
    private final OutboundPermissionRepository outboundPermissionRepository;
    private final ConnectionLimitDefaultRepository connectionLimitDefaultRepository;
    private final Clock clock;
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
            InboundPermissionRepository inboundPermissionRepository,
            OutboundPermissionRepository outboundPermissionRepository,
            ConnectionLimitDefaultRepository connectionLimitDefaultRepository,
            Clock clock,
            HandshakeService handshakeService,
            PermissionScheduler permissionScheduler,
            AuthService authService,
            DataSourceService dataSourceService,
            AiidaLocalDataNeedService aiidaLocalDataNeedService,
            AiidaEventPublisher aiidaEventPublisher,
            SecretsService secretsService
    ) {
        this.permissionRepository = permissionRepository;
        this.inboundPermissionRepository = inboundPermissionRepository;
        this.outboundPermissionRepository = outboundPermissionRepository;
        this.connectionLimitDefaultRepository = connectionLimitDefaultRepository;
        this.clock = clock;
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
     * an error during shutdown of the AiidaStreamer or sending of the permission market document occurs, they are
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

        if (!permission.status().isAccepted()) {
            throw new PermissionStateTransitionException(permission.id().toString(),
                                                         REVOKED.name(),
                                                         List.of(ACCEPTED.name()),
                                                         permission.status().name());
        }

        validateInboundDataSourceNotInUse(permission);

        Instant revocationTime = clock.instant();
        permission.setStatus(REVOKED);
        permission.setRevokeTime(revocationTime);

        permissionScheduler.stopPermission(permission, PermissionProcessStatus.REVOKED);
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
            var fetched = fetchValidatedDetails(permissionRequests.eddieId(),
                                                permissionId,
                                                permissionRequests.handshakeUrl(),
                                                permissionRequests.accessToken(),
                                                currentUserId);
            pendingPermissions.add(PendingPermissionDto.from(buildPermission(fetched)));
        }
        return pendingPermissions;
    }

    /**
     * Accepts the outbound permission request with the passed ID. The details are fetched and validated again, as the
     * client cannot be trusted. Afterward, the permission request is accepted at the EDDIE framework, and the
     * permission is persisted as {@link PermissionStatus#ACCEPTED} together with its MQTT credentials and the selected
     * data source, before data sharing is started or scheduled.
     *
     * @param permissionId The ID of the permission request that should be accepted.
     * @param acceptDto    Handshake information from the AIIDA code and the selections of the user.
     * @return The accepted and persisted permission.
     * @throws InvalidOutboundPermissionException The permission request is not an outbound permission request.
     * @throws DataSourceNotFoundException        The data source does not exist or belongs to another user.
     * @throws IncompatibleDataSourceException    The data source does not match the asset or schemas of the data need.
     * @throws DetailFetchingFailedException      Fetching the details or accepting at the EDDIE framework failed.
     */
    @Transactional
    public Permission acceptOutboundPermission(
            UUID permissionId,
            AcceptOutboundPermissionDto acceptDto
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException, InvalidOutboundPermissionException, DataSourceNotFoundException, IncompatibleDataSourceException, SecretStoringException {
        var fetched = fetchValidatedDetails(permissionId, acceptDto);
        if (!(fetched.dataNeed() instanceof OutboundAiidaLocalDataNeed dataNeed)) {
            throw new InvalidOutboundPermissionException(permissionId);
        }

        var permission = buildOutboundPermission(fetched, dataNeed);
        permission.setDataSource(validateDataSourceSelection(permission, acceptDto.dataSourceId()));
        permission = accept(permission, acceptDto);
        permissionScheduler.scheduleOrStart(AcceptedPermission.from(permission));
        return permission;
    }

    /**
     * Accepts the inbound permission request with the passed ID. The details are fetched and validated again, as the
     * client cannot be trusted. Afterward, the permission request is accepted at the EDDIE framework, and the
     * permission is persisted as {@link PermissionStatus#ACCEPTED} together with its MQTT credentials, before its data
     * source is created and data sharing is started or scheduled.
     *
     * @param permissionId The ID of the permission request that should be accepted.
     * @param acceptDto    Handshake information from the AIIDA code and the selections of the user.
     * @return The accepted and persisted permission.
     * @throws InvalidInboundPermissionException The permission request is not an inbound permission request.
     * @throws DetailFetchingFailedException     Fetching the details or accepting at the EDDIE framework failed.
     */
    @Transactional
    public Permission acceptInboundPermission(
            UUID permissionId,
            AcceptInboundPermissionDto acceptDto
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, InvalidUserException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException, LimitDefaultsNotAllowedException, InvalidInboundPermissionException, SecretStoringException {
        var fetched = fetchValidatedDetails(permissionId, acceptDto);
        if (!(fetched.dataNeed() instanceof InboundAiidaLocalDataNeed dataNeed)) {
            throw new InvalidInboundPermissionException(permissionId);
        }

        var permission = buildInboundPermission(fetched, dataNeed);

        var inboundMessageFormat = acceptDto.inboundMessageFormat();
        if (inboundMessageFormat != null) {
            permission.updateInboundMessageFormat(inboundMessageFormat);
        }

        permission = accept(permission, acceptDto);
        // creates the inbound data source, which is required to start the permission
        aiidaEventPublisher.publishEvent(new InboundPermissionAcceptEvent(permission.id()));
        permissionScheduler.scheduleOrStart(AcceptedPermission.from(permission));
        return permission;
    }

    @Transactional
    public Permission updateInboundMessageFormat(
            UUID permissionId,
            @Nullable InboundMessageFormat inboundMessageFormat
    ) throws PermissionNotFoundException, UnauthorizedException, InvalidUserException,
             MissingInboundMessageFormatException {
        if (inboundMessageFormat == null) {
            throw new MissingInboundMessageFormatException();
        }

        var permission = inboundPermissionRepository.findById(permissionId)
                                                    .orElseThrow(() -> new PermissionNotFoundException(permissionId));
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
        handshakeService.reject(HandshakeTarget.of(rejectDto.handshakeUrl(), permissionId, rejectDto.accessToken()),
                                rejectDto.reason());
    }

    public List<InboundPermission> getAcceptedInboundPermissions() throws InvalidUserException {
        var currentUserId = authService.getCurrentUserId();
        return inboundPermissionRepository.findByUserIdAndDataSourceIsNotNullAndStatusOrderByGrantTimeDesc(currentUserId,
                                                                                                          ACCEPTED);
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

    /**
     * Accepts the validated permission at the EDDIE framework and persists it together with its MQTT credentials.
     * Everything has to be validated before, to keep the window small in which EDDIE considers the permission
     * accepted, but AIIDA fails to persist it.
     */
    private <T extends Permission> T accept(
            T permission,
            AcceptPermissionDto acceptDto
    ) throws DetailFetchingFailedException, SecretStoringException {
        var displayName = acceptDto.displayName();
        if (displayName != null && !displayName.isBlank()) {
            permission.setDisplayName(displayName);
        }

        var permissionId = permission.id();
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

        // The MQTT streaming config references the permission, but is mapped via @PrimaryKeyJoinColumn, which makes
        // Hibernate insert it before the permission. Attaching it to the already persisted permission orders the
        // inserts correctly.
        permission = permissionRepository.save(permission);
        permission.setMqttStreamingConfig(mqttStreamingConfig);
        if (permission instanceof InboundPermission inboundPermission) {
            persistLimitDefaults(inboundPermission);
        }
        LOGGER.info("Permission ({}) accepted, will start or schedule data sharing", permission.id());
        return permission;
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
     * Fetches the details of the permission request from the EDDIE framework and validates what applies to both
     * directions: the permission request was not accepted yet, the data need type is supported, the start lies not in
     * the past and there is no other active flexible connection agreement for the meter.
     */
    private FetchedDetails fetchValidatedDetails(
            UUID eddieId,
            UUID permissionId,
            String handshakeUrlTemplate,
            String accessToken,
            UUID currentUserId
    ) throws PermissionAlreadyExistsException, DetailFetchingFailedException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException {
        if (permissionRepository.existsById(permissionId)) {
            throw new PermissionAlreadyExistsException(permissionId);
        }

        var target = HandshakeTarget.of(handshakeUrlTemplate, permissionId, accessToken);
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

        validateDataNeedType(target, permissionId, details.dataNeed().type());
        var dataNeedId = details.dataNeed().dataNeedId();
        var dataNeed = aiidaLocalDataNeedService.optionalAiidaLocalDataNeedById(dataNeedId)
                                                .orElseGet(() -> AiidaLocalDataNeedFactory.create(details.dataNeed()));
        var fetched = new FetchedDetails(eddieId, permissionId, currentUserId, target, details, dataNeed);

        validatePermissionStartInTheFuture(fetched);
        validateSingleActiveFcaPermissionPerMeterId(fetched);
        return fetched;
    }

    /**
     * Fetches and validates the details of the permission request from the AIIDA code passed to accept it, for the
     * current user.
     */
    private FetchedDetails fetchValidatedDetails(
            UUID permissionId,
            AcceptPermissionDto acceptDto
    ) throws InvalidUserException, PermissionAlreadyExistsException, DetailFetchingFailedException, ActiveFcaPermissionAlreadyExistsException, PermissionStartInThePastException, PermissionDataNeedTypeNotSupportedException {
        return fetchValidatedDetails(acceptDto.eddieId(),
                                     permissionId,
                                     acceptDto.handshakeUrl(),
                                     acceptDto.accessToken(),
                                     authService.getCurrentUserId());
    }

    /**
     * Builds the permission matching the direction of the data need.
     */
    private Permission buildPermission(FetchedDetails fetched) throws LimitDefaultsNotAllowedException {
        return switch (fetched.dataNeed()) {
            case InboundAiidaLocalDataNeed inboundDataNeed -> buildInboundPermission(fetched, inboundDataNeed);
            case OutboundAiidaLocalDataNeed outboundDataNeed -> buildOutboundPermission(fetched, outboundDataNeed);
            default -> throw new IllegalArgumentException("Unsupported data need type " + fetched.dataNeed().type());
        };
    }

    /**
     * Builds an inbound permission. Connection limit defaults are only allowed if the data need supports connection
     * limits, and they are only persisted once the permission is accepted.
     */
    private InboundPermission buildInboundPermission(
            FetchedDetails fetched,
            InboundAiidaLocalDataNeed dataNeed
    ) throws LimitDefaultsNotAllowedException {
        var details = fetched.details();
        var minLimitKw = details.minLimitKw();
        var maxLimitKw = details.maxLimitKw();
        var hasLimitDefaults = minLimitKw != null || maxLimitKw != null;
        var isRangeValid = minLimitKw == null || maxLimitKw == null || minLimitKw.compareTo(maxLimitKw) < 0;
        if (hasLimitDefaults && (!dataNeed.supportsConnectionLimits() || !isRangeValid)) {
            throw rejectLimitDefaults(fetched);
        }

        var permission = new InboundPermission(fetched.eddieId(),
                                               fetched.permissionId(),
                                               fetched.target().handshakeUrl(),
                                               fetched.target().accessToken(),
                                               fetched.userId(),
                                               dataNeed,
                                               details.connectionId(),
                                               fetched.startTime(),
                                               fetched.expirationTime(),
                                               clock.instant());
        permission.setMeterId(details.meterId());
        permission.setMinLimitKw(minLimitKw);
        permission.setMaxLimitKw(maxLimitKw);
        return permission;
    }

    /**
     * Builds an outbound permission. Connection limit defaults are not allowed for outbound permissions.
     */
    private OutboundPermission buildOutboundPermission(
            FetchedDetails fetched,
            OutboundAiidaLocalDataNeed dataNeed
    ) throws LimitDefaultsNotAllowedException {
        var details = fetched.details();
        if (details.minLimitKw() != null || details.maxLimitKw() != null) {
            throw rejectLimitDefaults(fetched);
        }

        var permission = new OutboundPermission(fetched.eddieId(),
                                                fetched.permissionId(),
                                                fetched.target().handshakeUrl(),
                                                fetched.target().accessToken(),
                                                fetched.userId(),
                                                dataNeed,
                                                details.connectionId(),
                                                fetched.startTime(),
                                                fetched.expirationTime(),
                                                clock.instant());
        permission.setMeterId(details.meterId());
        return permission;
    }

    private void persistLimitDefaults(InboundPermission permission) {
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

    private void validateDataNeedType(
            HandshakeTarget target,
            UUID permissionId,
            String dataNeedType
    ) throws PermissionDataNeedTypeNotSupportedException {
        if (!isValidDataNeedType(dataNeedType)) {
            var exception = new PermissionDataNeedTypeNotSupportedException(permissionId, dataNeedType);
            denotePermissionAsUnfulfillable(target, permissionId, exception.getMessage());
            throw exception;
        }
    }

    private void validatePermissionStartInTheFuture(FetchedDetails fetched) throws PermissionStartInThePastException {
        var start = fetched.startTime().atZone(AIIDA_ZONE_ID);
        var today = LocalDate.now(AIIDA_ZONE_ID).atStartOfDay(AIIDA_ZONE_ID);
        if (start.isBefore(today)) {
            var exception = new PermissionStartInThePastException(fetched.permissionId(), start);
            denotePermissionAsUnfulfillable(fetched.target(), fetched.permissionId(), exception.getMessage());
            throw exception;
        }
    }

    private void validateSingleActiveFcaPermissionPerMeterId(FetchedDetails fetched) throws ActiveFcaPermissionAlreadyExistsException {
        var dataNeed = fetched.dataNeed();
        if (!dataNeed.contexts().contains(AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT)) {
            return;
        }

        var meterId = fetched.details().meterId();
        if (meterId == null || meterId.isBlank()) {
            return;
        }

        var hasConflict = permissionRepository.existsByUserIdAndDataNeedTypeAndContextAndMeterIdAndStatus(fetched.userId(),
                                                                                                            dataNeed.type(),
                                                                                                            AiidaContext.FLEXIBLE_CONNECTION_AGREEMENT,
                                                                                                            meterId,
                                                                                                            ACCEPTED);
        if (hasConflict) {
            var exception = new ActiveFcaPermissionAlreadyExistsException(fetched.permissionId(),
                                                                          meterId,
                                                                          dataNeed.type());
            denotePermissionAsUnfulfillable(fetched.target(), fetched.permissionId(), exception.getMessage());
            throw exception;
        }
    }

    private LimitDefaultsNotAllowedException rejectLimitDefaults(FetchedDetails fetched) {
        var exception = new LimitDefaultsNotAllowedException(fetched.permissionId());
        denotePermissionAsUnfulfillable(fetched.target(), fetched.permissionId(), exception.getMessage());
        return exception;
    }

    private boolean isValidDataNeedType(String dataNeedType) {
        return OutboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeedType)
               || InboundAiidaDataNeed.DISCRIMINATOR_VALUE.equals(dataNeedType);
    }

    private void denotePermissionAsUnfulfillable(HandshakeTarget target, UUID permissionId, @Nullable String reason) {
        LOGGER.info("Permission request {} cannot be fulfilled: {}", permissionId, reason);
        handshakeService.denoteUnfulfillable(target, reason);
    }

    private DataSource validateDataSourceSelection(OutboundPermission permission, UUID dataSourceId)
            throws DataSourceNotFoundException, IncompatibleDataSourceException {
        var dataSource = dataSourceService.dataSourceByIdOrThrow(dataSourceId);

        if (!Objects.equals(dataSource.userId(), permission.userId())) {
            throw new DataSourceNotFoundException(dataSourceId);
        }

        // inbound data sources have no asset and may be forwarded regardless of the requested assets
        var requiredAssets = permission.dataNeed().assets();
        if (!(dataSource instanceof InboundDataSource)
            && !requiredAssets.isEmpty()
            && !requiredAssets.contains(dataSource.asset())) {
            throw new IncompatibleDataSourceException(
                    dataSourceId,
                    "The selected data source has asset '%s', but the data need requires one of %s.".formatted(
                            dataSource.asset(),
                            requiredAssets)
            );
        }

        if (dataSource instanceof InboundDataSource inboundDataSource) {
            var requiredSchemas = permission.dataNeed().schemas();
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
            var permissions = outboundPermissionRepository.findByDataSourceIdAndStatus(dataSourceId, ACCEPTED);
            if (!permissions.isEmpty()) {
                var blockingIds = permissions.stream().map(OutboundPermission::id).toList();
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
        return permission instanceof InboundPermission
               && permission.dataSource() instanceof InboundDataSource inboundDataSource
               && permission.equals(inboundDataSource.permission()) ? inboundDataSource.id() : null;
    }

    /**
     * Details of a permission request fetched from the EDDIE framework, together with the context from the AIIDA code.
     */
    private record FetchedDetails(
            UUID eddieId,
            UUID permissionId,
            UUID userId,
            HandshakeTarget target,
            PermissionDetailsDto details,
            AiidaLocalDataNeed dataNeed
    ) {
        Instant startTime() {
            return ZonedDateTime.of(details.start(), LocalTime.MIN, AIIDA_ZONE_ID).toInstant();
        }

        Instant expirationTime() {
            return ZonedDateTime.of(details.end(), LocalTime.MAX.withNano(0), AIIDA_ZONE_ID).toInstant();
        }
    }
}
