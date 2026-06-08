// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.api.agnostic.aiida.AiidaConnectionStatusMessageDto;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Instant;

import static energy.eddie.aiida.models.permission.PermissionStatus.TERMINATED;
import static java.util.Objects.requireNonNull;

/**
 * Handles {@link PermissionCommand}s that the EP sends to a streamer. The {@link energy.eddie.aiida.streamers.mqtt.MqttStreamer}
 * only parses and forwards the commands; all validation, persistence and side effects live here so the streamer stays
 * free of command logic.
 */
@Service
public class PermissionCommandService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCommandService.class);

    private final PermissionRepository permissionRepository;
    private final StreamerManager streamerManager;
    private final Clock clock;
    private final PermissionScheduler permissionScheduler;
    private final AiidaEventPublisher aiidaEventPublisher;
    private final PermissionCommandService self;

    @Autowired
    public PermissionCommandService(
            PermissionRepository permissionRepository,
            StreamerManager streamerManager,
            Clock clock,
            PermissionScheduler permissionScheduler,
            AiidaEventPublisher aiidaEventPublisher,
            @Lazy PermissionCommandService self
    ) {
        this.permissionRepository = permissionRepository;
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
        if (action.requiresExplicitGrant() && !isPermissionCommandAllowed(permission, action)) {
            LOGGER.warn("Rejected command {} for permission {}: command not in allowed permission commands for this data need",
                        action,
                        permissionId);
            return;
        }

        switch (command) {
            case PermissionCommand.Terminate ignored -> terminate(permission);
            case PermissionCommand.SetTransmissionEnabled setTransmissionEnabled ->
                    setTransmissionEnabled(permission, setTransmissionEnabled.enabled());
            case PermissionCommand.UpdateTransmissionSchedule updateTransmissionSchedule ->
                    updateSchedule(permission, updateTransmissionSchedule.transmissionSchedule());
        }
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
        var dataNeed = permission.dataNeed();
        return dataNeed != null
                && dataNeed.allowedPermissionCommands() != null
                && dataNeed.allowedPermissionCommands().contains(action);
    }

    private void setTransmissionEnabled(Permission permission, boolean enabled) {
        permission.setTransmissionEnabled(enabled);
        permissionRepository.save(permission);
        streamerManager.setTransmissionEnabled(permission.id(), enabled);
        LOGGER.info("Set transmissionEnabled to {} for permission {}", enabled, permission.id());
    }

    private void updateSchedule(Permission permission, String transmissionSchedule) {
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
        streamerManager.updateSchedule(permission);
        LOGGER.info("Updated transmission schedule to {} for permission {}", cron, permission.id());
    }

    private void terminate(Permission permission) {
        LOGGER.info("Will handle termination request for permission '{}'.", permission.id());

        permissionScheduler.removePermission(permission.id());

        Instant terminateTime = clock.instant();
        permission.setRevokeTime(terminateTime);
        permission.setStatus(TERMINATED);
        permission = permissionRepository.save(permission);

        var dataNeedId = requireNonNull(permission.dataNeed()).dataNeedId();
        var connectionId = requireNonNull(permission.connectionId());
        var terminatedMessage = new AiidaConnectionStatusMessageDto(connectionId,
                                                                    dataNeedId,
                                                                    clock.instant(),
                                                                    PermissionProcessStatus.EXTERNALLY_TERMINATED,
                                                                    permission.id(),
                                                                    permission.eddieId());
        streamerManager.stopStreamer(terminatedMessage);

        removeInboundDataSourceIfExists(permission);
    }

    private void removeInboundDataSourceIfExists(Permission permission) {
        var dataSource = permission.dataSource();

        if (dataSource != null && dataSource.type() == DataSourceType.INBOUND) {
            aiidaEventPublisher.publishEvent(new InboundPermissionRevokeEvent(dataSource.id()));
            permission.setDataSource(null);
        }
    }
}
