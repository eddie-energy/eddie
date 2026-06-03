// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.AiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.support.CronExpression;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyBoolean;

@ExtendWith(MockitoExtension.class)
class PermissionCommandServiceTest {
    private final UUID permissionId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataNeedId = UUID.fromString("92831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID eddieId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataSourceId = UUID.fromString("40201e2c-a01c-41b8-9db6-3f51670df7a5");
    private final String regionConnectorId = "rc-1";
    private final String connectionId = "conn-1";
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00.00Z"), ZoneOffset.UTC);

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionScheduler permissionScheduler;
    @Mock
    private AiidaEventPublisher aiidaEventPublisher;
    @Mock
    private Permission permission;
    @Mock
    private AiidaLocalDataNeed dataNeed;
    @Mock
    private DataSource dataSource;
    @Mock
    private PermissionCommandService self;

    private PermissionCommandService service;

    @BeforeEach
    void setUp() {
        service = new PermissionCommandService(permissionRepository,
                                               streamerManager,
                                               clock,
                                               permissionScheduler,
                                               aiidaEventPublisher,
                                               self);
    }

    @Test
    void givenUnknownPermission_handleCommand_doesNothing() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        service.handleCommand(new PermissionCommand.Terminate(regionConnectorId, permissionId));

        verify(streamerManager, never()).stopStreamer(any());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void givenTransmissionControlNotAllowed_setTransmissionEnabled_isRejected() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of());

        service.handleCommand(new PermissionCommand.SetTransmissionEnabled(regionConnectorId, permissionId, false));

        verify(permission, never()).setTransmissionEnabled(anyBoolean());
        verify(permissionRepository, never()).save(any());
        verify(streamerManager, never()).setTransmissionEnabled(any(), anyBoolean());
    }

    @Test
    void givenTransmissionControlAllowed_setTransmissionEnabled_persistsAndTogglesStreamer() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.id()).thenReturn(permissionId);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.SET_TRANSMISSION_ENABLED));

        service.handleCommand(new PermissionCommand.SetTransmissionEnabled(regionConnectorId, permissionId, false));

        verify(permission).setTransmissionEnabled(false);
        verify(permissionRepository).save(permission);
        verify(streamerManager).setTransmissionEnabled(permissionId, false);
    }

    @Test
    void givenValidSchedule_updateSchedule_persistsAndRebuildsFlux() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE));

        service.handleCommand(new PermissionCommand.UpdateTransmissionSchedule(regionConnectorId, permissionId, "*/30 * * * * *"));

        verify(permission).setTransmissionSchedule(argThat(cron -> cron.toString().equals("*/30 * * * * *")));
        verify(permissionRepository).save(permission);
        verify(streamerManager).updateSchedule(eq(permission), any(CronExpression.class));
    }

    @Test
    void givenInvalidScheduleCron_updateSchedule_isIgnored() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE));

        service.handleCommand(new PermissionCommand.UpdateTransmissionSchedule(regionConnectorId, permissionId, "not-a-cron"));

        verify(permission, never()).setTransmissionSchedule(any());
        verify(permissionRepository, never()).save(any());
        verify(streamerManager, never()).updateSchedule(any(), any());
    }

    @Test
    void givenTerminate_handleCommand_stopsStreamerAndUpdatesStatus() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(permission.id()).thenReturn(permissionId);
        when(permission.eddieId()).thenReturn(eddieId);
        when(permission.connectionId()).thenReturn(connectionId);
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.dataNeedId()).thenReturn(dataNeedId);
        when(permission.dataSource()).thenReturn(dataSource);
        when(dataSource.type()).thenReturn(DataSourceType.INBOUND);
        when(dataSource.id()).thenReturn(dataSourceId);

        service.handleCommand(new PermissionCommand.Terminate(regionConnectorId, permissionId));

        verify(permissionScheduler).removePermission(permissionId);
        verify(permission).setStatus(PermissionStatus.TERMINATED);
        verify(permission).setRevokeTime(any());
        verify(streamerManager).stopStreamer(
                argThat(msg -> msg.status() == PermissionProcessStatus.EXTERNALLY_TERMINATED));
        verify(aiidaEventPublisher).publishEvent(new InboundPermissionRevokeEvent(dataSourceId));
        verify(permission).setDataSource(null);
    }
}
