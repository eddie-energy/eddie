// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.InboundPermissionEndEvent;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.MqttStreamingConfig;
import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.models.permission.PermissionStatus;
import energy.eddie.aiida.models.permission.dataneed.InboundAiidaLocalDataNeed;
import energy.eddie.aiida.models.permission.dataneed.OutboundAiidaLocalDataNeed;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.ConnectionLimitDefaultRepository;
import energy.eddie.aiida.repositories.PermissionRepository;
import energy.eddie.aiida.streamers.StreamerManager;
import energy.eddie.cim.agnostic.PermissionCommand;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionCommandServiceTest {
    private final UUID permissionId = UUID.fromString("82831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID eddieId = UUID.fromString("72831e2c-a01c-41b8-9db6-3f51670df7a5");
    private final UUID dataSourceId = UUID.fromString("40201e2c-a01c-41b8-9db6-3f51670df7a5");
    private final String regionConnectorId = "rc-1";
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-03T10:00:00.00Z"), ZoneOffset.UTC);

    @Mock
    private PermissionRepository permissionRepository;
    @Mock
    private ConnectionLimitDefaultRepository connectionLimitDefaultRepository;
    @Mock
    private StreamerManager streamerManager;
    @Mock
    private PermissionScheduler permissionScheduler;
    @Mock
    private AiidaEventPublisher aiidaEventPublisher;
    @Mock
    private OutboundPermission permission;
    @Mock
    private OutboundAiidaLocalDataNeed dataNeed;
    @Mock
    private InboundPermission inboundPermission;
    @Mock
    private InboundAiidaLocalDataNeed inboundDataNeed;
    @Mock
    private InboundDataSource dataSource;
    @Mock
    private MqttStreamingConfig mqttStreamingConfig;
    @Mock
    private PermissionCommandService self;

    private PermissionCommandService service;

    @BeforeEach
    void setUp() {
        service = new PermissionCommandService(permissionRepository, connectionLimitDefaultRepository,
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

        verify(permissionScheduler, never()).stopPermission(any(), any());
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
        var acceptedPermission = acceptedPermission();
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(acceptedPermission));
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE));

        service.handleCommand(new PermissionCommand.UpdateTransmissionSchedule(regionConnectorId,
                                                                               permissionId,
                                                                               "*/30 * * * * *"));

        verify(permissionRepository).save(acceptedPermission);
        verify(streamerManager).updateSchedule(argThat(snapshot ->
                                                               snapshot.id().equals(permissionId)
                                                               && snapshot.transmissionSchedule()
                                                                          .toString()
                                                                          .equals("*/30 * * * * *")));
    }

    @Test
    void givenInvalidScheduleCron_updateSchedule_isIgnored() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE));

        service.handleCommand(new PermissionCommand.UpdateTransmissionSchedule(regionConnectorId,
                                                                               permissionId,
                                                                               "not-a-cron"));

        verify(permission, never()).setTransmissionSchedule(any());
        verify(permissionRepository, never()).save(any());
        verify(streamerManager, never()).updateSchedule(any());
    }

    @Test
    void givenValidLimitDefaults_updateLimitDefaults_savesOpenDefault() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.id()).thenReturn(permissionId);
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_LIMIT_DEFAULTS));
        when(inboundPermission.supportsConnectionLimits()).thenReturn(true);

        service.handleCommand(new PermissionCommand.UpdateLimitDefaults(regionConnectorId,
                                                                        permissionId,
                                                                        BigDecimal.ONE,
                                                                        BigDecimal.TEN));

        verify(connectionLimitDefaultRepository).closeOpenDefaults(permissionId, clock.instant());
        verify(connectionLimitDefaultRepository)
                .save(argThat(defaultLimit ->
                                      defaultLimit.permissionId().equals(permissionId) &&
                                      defaultLimit.start().equals(clock.instant()) &&
                                      defaultLimit.end() == null &&
                                      Objects.requireNonNull(defaultLimit.minLimitKw())
                                             .compareTo(BigDecimal.ONE) == 0 &&
                                      Objects.requireNonNull(defaultLimit.maxLimitKw())
                                             .compareTo(BigDecimal.TEN) == 0));
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void givenNullLimitDefaults_updateLimitDefaults_closesOpenDefaultWithoutNewRow() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.id()).thenReturn(permissionId);
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_LIMIT_DEFAULTS));
        when(inboundPermission.supportsConnectionLimits()).thenReturn(true);

        service.handleCommand(new PermissionCommand.UpdateLimitDefaults(regionConnectorId, permissionId, null, null));

        verify(connectionLimitDefaultRepository).closeOpenDefaults(permissionId, clock.instant());
        verify(connectionLimitDefaultRepository, never()).save(any());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void givenNotEligibleForLimitDefaults_updateLimitDefaults_isIgnored() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_LIMIT_DEFAULTS));
        when(inboundPermission.supportsConnectionLimits()).thenReturn(false);

        service.handleCommand(new PermissionCommand.UpdateLimitDefaults(regionConnectorId,
                                                                        permissionId,
                                                                        BigDecimal.ONE,
                                                                        BigDecimal.TEN));

        verify(connectionLimitDefaultRepository, never()).closeOpenDefaults(any(), any());
        verify(connectionLimitDefaultRepository, never()).save(any());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void givenMinNotLessThanMax_updateLimitDefaults_isIgnored() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_LIMIT_DEFAULTS));
        when(inboundPermission.supportsConnectionLimits()).thenReturn(true);

        service.handleCommand(new PermissionCommand.UpdateLimitDefaults(regionConnectorId, permissionId, BigDecimal.TEN, BigDecimal.ONE));

        verify(connectionLimitDefaultRepository, never()).closeOpenDefaults(any(), any());
        verify(connectionLimitDefaultRepository, never()).save(any());
        verify(permissionRepository, never()).save(any());
    }

    @Test
    void givenInboundPermission_setTransmissionEnabled_isRejected() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.SET_TRANSMISSION_ENABLED));

        service.handleCommand(new PermissionCommand.SetTransmissionEnabled(regionConnectorId, permissionId, false));

        verify(permissionRepository, never()).save(any());
        verify(streamerManager, never()).setTransmissionEnabled(any(), anyBoolean());
    }

    @Test
    void givenInboundPermission_updateSchedule_isRejected() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(inboundPermission.dataNeed()).thenReturn(inboundDataNeed);
        when(inboundDataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_TRANSMISSION_SCHEDULE));

        service.handleCommand(new PermissionCommand.UpdateTransmissionSchedule(regionConnectorId, permissionId, "*/30 * * * * *"));

        verify(permissionRepository, never()).save(any());
        verify(streamerManager, never()).updateSchedule(any());
    }

    @Test
    void givenOutboundPermission_updateLimitDefaults_isIgnored() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permission.dataNeed()).thenReturn(dataNeed);
        when(dataNeed.allowedPermissionCommands()).thenReturn(Set.of(PermissionCommand.Action.UPDATE_LIMIT_DEFAULTS));

        service.handleCommand(new PermissionCommand.UpdateLimitDefaults(regionConnectorId,
                                                                        permissionId,
                                                                        BigDecimal.ONE,
                                                                        BigDecimal.TEN));

        verify(connectionLimitDefaultRepository, never()).closeOpenDefaults(any(), any());
        verify(connectionLimitDefaultRepository, never()).save(any());
    }

    @Test
    void givenTerminate_handleCommand_stopsStreamerAndUpdatesStatus() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(inboundPermission));
        when(permissionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(inboundPermission.id()).thenReturn(permissionId);
        when(inboundPermission.dataSource()).thenReturn(dataSource);
        when(dataSource.id()).thenReturn(dataSourceId);

        service.handleCommand(new PermissionCommand.Terminate(regionConnectorId, permissionId));

        verify(inboundPermission).setStatus(PermissionStatus.TERMINATED);
        verify(inboundPermission).setRevokeTime(any());
        verify(permissionScheduler).stopPermission(inboundPermission, PermissionProcessStatus.EXTERNALLY_TERMINATED);
        verify(aiidaEventPublisher).publishEvent(new InboundPermissionEndEvent(dataSourceId));
        verify(inboundPermission).setDataSource(null);
    }

    @Test
    void givenOutboundPermissionUsingInboundDataSource_terminate_doesNotRemoveInboundDataSource() {
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(permissionRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
        when(permission.id()).thenReturn(permissionId);
        lenient().when(permission.dataSource()).thenReturn(dataSource);
        lenient().when(dataSource.type()).thenReturn(DataSourceType.INBOUND);

        service.handleCommand(new PermissionCommand.Terminate(regionConnectorId, permissionId));

        verify(permission).setStatus(PermissionStatus.TERMINATED);
        verify(aiidaEventPublisher, never()).publishEvent(any());
        verify(permission, never()).setDataSource(any());
    }

    private OutboundPermission acceptedPermission() {
        var acceptedPermission = new OutboundPermission(eddieId,
                                                        permissionId,
                                                        "https://example.org",
                                                        "token",
                                                        UUID.randomUUID(),
                                                        dataNeed,
                                                        "conn-1",
                                                        clock.instant(),
                                                        clock.instant().plusSeconds(3600),
                                                        Instant.parse("2026-01-01T00:00:00Z"));
        acceptedPermission.setDataSource(dataSource);
        acceptedPermission.setMqttStreamingConfig(mqttStreamingConfig);
        return acceptedPermission;
    }
}
