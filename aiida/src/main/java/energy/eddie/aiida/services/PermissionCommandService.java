// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionEndEvent;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.connectionlimit.ConnectionLimitDefault;
import energy.eddie.aiida.models.permission.AcceptedPermission;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;

import static energy.eddie.aiida.models.permission.PermissionStatus.TERMINATED;

/**
 * Handles {@link PermissionCommand}s that the EP sends to a streamer. The {@link energy.eddie.aiida.streamers.mqtt.MqttStreamer}
 * only parses and forwards the commands; all validation, persistence and side effects live here so the streamer stays
 * free of command logic.
 */
@Service
public class PermissionCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCommandService.class);

    private final PermissionRepository permissionRepository;
    private final ConnectionLimitDefaultRepository connectionLimitDefaultRepository;
    private final StreamerManager streamerManager;
    private final Clock clock;
    private final PermissionScheduler permissionScheduler;
    private final AiidaEventPublisher aiidaEventPublisher;
    private final PermissionCommandService self;

    @Autowired
    public PermissionCommandService(
            PermissionRepository permissionRepository,
            ConnectionLimitDefaultRepository connectionLimitDefaultRepository,
            StreamerManager streamerManager,
            Clock clock,
            PermissionScheduler permissionScheduler,
            AiidaEventPublisher aiidaEventPublisher,
            @Lazy PermissionCommandService self
    ) {
        this.permissionRepository = permissionRepository;
        this.connectionLimitDefaultRepository = connectionLimitDefaultRepository;
        this.streamerManager = streamerManager;
        this.clock = clock;
        this.permissionScheduler = permissionScheduler;
        this.aiidaEventPublisher = aiidaEventPublisher;
        this.self = self; // proxy ref so @Transactional applies on internal call
    }

    /**
     * Validates and applies a single {@link PermissionCommand} received from the EP.
     */
    @Transactional
    public void handleCommand(PermissionCommand command) {
        var permissionId = command.permissionId();
        LOGGER.info("Handling permission command {} for permission {}", command.action(), permissionId);

        Permission permission;
        try {
            permission = permissionRepository.findById(permissionId)
                                             .orElseThrow(() -> new PermissionNotFoundException(permissionId));
        } catch (PermissionNotFoundException e) {
            LOGGER.error("Received command {} for permission {}, but it cannot be found in the database",
                         command.action(),
                         permissionId);
            return;
        }

        var action = command.action();
        if (!isPermissionCommandAllowed(permission, action)) {
            LOGGER.warn("Rejected command {} for permission {}: command not in allowed permission commands for this data need",
                        action,
                        permissionId);
            return;
        }

        switch (command) {
            case PermissionCommand.Terminate ignored -> terminate(permission);
            case PermissionCommand.SetTransmissionEnabled setTransmissionEnabled -> {
                if (permission instanceof OutboundPermission outboundPermission) {
                    setTransmissionEnabled(outboundPermission, setTransmissionEnabled.enabled());
                } else {
                    rejectWrongDirection(permission, action, OutboundPermission.DISCRIMINATOR_VALUE);
                }
            }
            case PermissionCommand.UpdateTransmissionSchedule updateTransmissionSchedule -> {
                if (permission instanceof OutboundPermission outboundPermission) {
                    updateSchedule(outboundPermission, updateTransmissionSchedule.transmissionSchedule());
                } else {
                    rejectWrongDirection(permission, action, OutboundPermission.DISCRIMINATOR_VALUE);
                }
            }
            case PermissionCommand.UpdateLimitDefaults updateLimitDefaults -> {
                if (permission instanceof InboundPermission inboundPermission) {
                    updateLimitDefaults(inboundPermission,
                                        updateLimitDefaults.minLimitKw(),
                                        updateLimitDefaults.maxLimitKw());
                } else {
                    rejectWrongDirection(permission, action, InboundPermission.DISCRIMINATOR_VALUE);
                }
            }
        }
    }

    private static void rejectWrongDirection(
            Permission permission,
            PermissionCommand.Action action,
            String requiredDirection
    ) {
        LOGGER.warn("Rejected command {} for permission {}: permission is not {}",
                    action,
                    permission.id(),
                    requiredDirection);
    }

    @PostConstruct
    void subscribeToCommands() {
        streamerManager.commandFlux()
                       .publishOn(Schedulers.boundedElastic())
                       .doOnNext(self::handleCommand)
                       .onErrorContinue((error, command) ->
                                                LOGGER.error(
                                                        "Unexpected error handling command {}; command dropped, subscription kept alive",
                                                        command,
                                                        error))
                       .subscribe();
    }

    private boolean isPermissionCommandAllowed(Permission permission, PermissionCommand.Action action) {
        if (!action.requiresExplicitGrant()) {
            return true;
        }

        var dataNeed = permission.dataNeed();
        return dataNeed.allowedPermissionCommands() != null
                && dataNeed.allowedPermissionCommands().contains(action);
    }

    private void setTransmissionEnabled(OutboundPermission permission, boolean enabled) {
        permission.setTransmissionEnabled(enabled);
        permissionRepository.save(permission);
        streamerManager.setTransmissionEnabled(permission.id(), enabled);
        LOGGER.info("Set transmissionEnabled to {} for permission {}", enabled, permission.id());
    }

    private void updateSchedule(OutboundPermission permission, String transmissionSchedule) {
        CronExpression cron;
        try {
            cron = CronExpression.parse(transmissionSchedule);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Rejected UPDATE_SCHEDULE for permission {}: invalid cron expression '{}'",
                        permission.id(),
                        transmissionSchedule,
                        e);
            return;
        }

        permission.setTransmissionSchedule(cron);
        permissionRepository.save(permission);
        if (permission.status().isAccepted()) {
            streamerManager.updateSchedule(AcceptedPermission.from(permission));
        }
        LOGGER.info("Updated transmission schedule to {} for permission {}", cron, permission.id());
    }

    private void updateLimitDefaults(
            InboundPermission permission,
            @Nullable BigDecimal minLimitKw,
            @Nullable BigDecimal maxLimitKw
    ) {
        if (!permission.supportsConnectionLimits()) {
            LOGGER.warn(
                    "Rejected UPDATE_LIMIT_DEFAULTS for permission {}: data need does not have the MIN_MAX_ENVELOPE_CIM_V1_12 schema",
                    permission.id());
            return;
        }

        var isRangeValid = minLimitKw == null || maxLimitKw == null || minLimitKw.compareTo(maxLimitKw) < 0;
        if (!isRangeValid) {
            LOGGER.warn(
                    "Rejected UPDATE_LIMIT_DEFAULTS for permission {}: minLimitKw {} must be less than maxLimitKw {}",
                    permission.id(),
                    minLimitKw,
                    maxLimitKw);
            return;
        }

        var now = clock.instant();
        connectionLimitDefaultRepository.closeOpenDefaults(permission.id(), now);
        if (minLimitKw != null || maxLimitKw != null) {
            var defaultLimit = new ConnectionLimitDefault(permission.id(),
                                                          permission.meterId(),
                                                          now,
                                                          null,
                                                          minLimitKw,
                                                          maxLimitKw);
            connectionLimitDefaultRepository.save(defaultLimit);
        }
        LOGGER.info("Updated limit defaults to min {} and max {} for permission {}",
                    minLimitKw,
                    maxLimitKw,
                    permission.id());
    }

    private void terminate(Permission permission) {
        LOGGER.info("Will handle termination request for permission '{}'.", permission.id());

        Instant terminateTime = clock.instant();
        permission.setRevokeTime(terminateTime);
        permission.setStatus(TERMINATED);
        permission = permissionRepository.save(permission);

        permissionScheduler.stopPermission(permission, PermissionProcessStatus.EXTERNALLY_TERMINATED);

        removeInboundDataSourceIfExists(permission);
    }

    private void removeInboundDataSourceIfExists(Permission permission) {
        // outbound permissions may stream from the inbound data source of another permission
        var dataSource = permission instanceof InboundPermission inboundPermission ? inboundPermission.dataSource() : null;
        if (dataSource != null) {
            aiidaEventPublisher.publishEvent(new InboundPermissionEndEvent(dataSource.id()));
            permission.setDataSource(null);
        }
    }
}
