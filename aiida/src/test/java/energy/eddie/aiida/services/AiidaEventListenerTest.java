// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionEndEvent;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.inbound.MissingMqttStreamingConfigException;
import energy.eddie.aiida.errors.permission.InboundDataSourceInUseException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.InboundPermission;
import energy.eddie.aiida.models.permission.OutboundPermission;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.aiida.repositories.InboundPermissionRepository;
import energy.eddie.aiida.repositories.OutboundPermissionRepository;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import energy.eddie.cim.agnostic.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static energy.eddie.aiida.models.permission.PermissionStatus.ACCEPTED;
import static energy.eddie.aiida.models.permission.PermissionStatus.FULFILLED;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AiidaEventListener.class, AiidaEventPublisher.class})
class AiidaEventListenerTest {
    @Autowired
    private AiidaEventPublisher aiidaEventPublisher;
    @MockitoBean
    private DataSourceService dataSourceService;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoBean
    private ProvisioningService provisioningService;
    @MockitoBean
    private InboundPermissionRepository inboundPermissionRepository;
    @MockitoBean
    private OutboundPermissionRepository outboundPermissionRepository;
    @MockitoBean
    private PermissionScheduler permissionScheduler;
    @MockitoSpyBean
    @InjectMocks
    private AiidaEventListener aiidaEventListener;

    @Test
    void testOnInboundPermissionAcceptEvent() throws SecretStoringException, SecretLoadingException {
        // Given
        var event = mock(InboundPermissionAcceptEvent.class);
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permission = mock(InboundPermission.class);
        var inboundDataSource = mock(InboundDataSource.class);

        // When
        when(event.permissionId()).thenReturn(permissionId);
        when(inboundPermissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(dataSourceService.createInboundDataSource(permission)).thenReturn(inboundDataSource);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).createInboundDataSource(event);
        verify(inboundPermissionRepository).findById(permissionId);
        verify(dataSourceService).createInboundDataSource(permission);
        verify(permission).setDataSource(inboundDataSource);
        verify(dataSourceService).startDataSource(inboundDataSource);
    }

    @Test
    void testOnInboundPermissionAcceptEvent_missingMqttStreamingConfigIsHandled()
            throws SecretStoringException, SecretLoadingException {
        var event = mock(InboundPermissionAcceptEvent.class);
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permission = mock(InboundPermission.class);

        when(event.permissionId()).thenReturn(permissionId);
        when(inboundPermissionRepository.findById(permissionId)).thenReturn(Optional.of(permission));
        when(dataSourceService.createInboundDataSource(permission))
                .thenThrow(new MissingMqttStreamingConfigException(null));

        assertThatCode(() -> aiidaEventPublisher.publishEvent(event)).doesNotThrowAnyException();

        verify(aiidaEventListener).createInboundDataSource(event);
        verify(dataSourceService).createInboundDataSource(permission);
        verify(permission, never()).setDataSource(any());
        verify(dataSourceService, never()).startDataSource(any());
    }

    @Test
    void testOnInboundPermissionAcceptEvent_missingInboundPermissionIsHandled() {
        var event = mock(InboundPermissionAcceptEvent.class);
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        when(event.permissionId()).thenReturn(permissionId);
        when(inboundPermissionRepository.findById(permissionId)).thenReturn(Optional.empty());

        assertThatCode(() -> aiidaEventPublisher.publishEvent(event)).doesNotThrowAnyException();

        verifyNoInteractions(dataSourceService);
    }

    @Test
    void testOnInboundPermissionEndEvent() {
        // Given
        var event = mock(InboundPermissionEndEvent.class);
        var dataSourceId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When
        when(event.dataSourceId()).thenReturn(dataSourceId);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).deleteInboundDataSource(event);
        verify(provisioningService).stopPublisher(dataSourceId);
        verify(dataSourceService).deleteDataSource(dataSourceId);
        verify(permissionScheduler, never()).stopPermission(any(), any());
    }

    @Test
    void testOnInboundPermissionEndEvent_fulfillsForwardingOutboundPermissions() {
        // Given
        var dataSourceId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var event = new InboundPermissionEndEvent(dataSourceId);
        var outboundPermission = mock(OutboundPermission.class);
        when(outboundPermissionRepository.findByDataSourceIdAndStatus(dataSourceId, ACCEPTED))
                .thenReturn(List.of(outboundPermission));

        // When
        aiidaEventPublisher.publishEvent(event);

        // Then
        var inOrder = inOrder(outboundPermission, permissionScheduler, dataSourceService);
        inOrder.verify(outboundPermission).setStatus(FULFILLED);
        inOrder.verify(permissionScheduler).stopPermission(outboundPermission, PermissionProcessStatus.FULFILLED);
        inOrder.verify(dataSourceService).deleteDataSource(dataSourceId);
    }

    @Test
    void testOnDataSourceDeletionEvent() throws UnauthorizedException, PermissionNotFoundException,
                                                PermissionStateTransitionException, InvalidUserException,
                                                InboundDataSourceInUseException {
        // Given
        var event = mock(DataSourceDeletionEvent.class);
        var permissionId1 = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permissionId2 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var permissionIds = Set.of(permissionId1, permissionId2);

        // When
        when(event.permissionIds()).thenReturn(permissionIds);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).revokeAllAssociatedPermissions(event);
        verify(permissionService, times(permissionIds.size())).revokePermission(any(UUID.class));
    }
}
