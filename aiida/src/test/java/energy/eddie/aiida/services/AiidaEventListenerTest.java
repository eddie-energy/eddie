// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionAcceptEvent;
import energy.eddie.aiida.dtos.events.InboundPermissionRevokeEvent;
import energy.eddie.aiida.dtos.events.OutboundPermissionAcceptEvent;
import energy.eddie.aiida.errors.SecretLoadingException;
import energy.eddie.aiida.errors.SecretStoringException;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.datasource.DataSourceNotFoundException;
import energy.eddie.aiida.errors.inbound.MissingMqttStreamingConfigException;
import energy.eddie.aiida.errors.permission.InboundDataSourceInUseException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.mqtt.inbound.InboundDataSource;
import energy.eddie.aiida.models.permission.Permission;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
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
    @MockitoSpyBean
    @InjectMocks
    private AiidaEventListener aiidaEventListener;

    @Test
    void testOnOutboundPermissionAcceptEvent() throws DataSourceNotFoundException {
        // Given
        var event = mock(OutboundPermissionAcceptEvent.class);
        var dataSourceId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var dataSource = mock(DataSource.class);

        // When
        when(event.dataSourceId()).thenReturn(dataSourceId);
        when(event.permissionId()).thenReturn(permissionId);
        when(dataSourceService.dataSourceByIdOrThrow(dataSourceId)).thenReturn(dataSource);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).addDataSourceToPermission(event);
        verify(dataSourceService).dataSourceByIdOrThrow(dataSourceId);
        verify(permissionService).addDataSourceToPermission(dataSource, permissionId);
    }

    @Test
    void testOnInboundPermissionAcceptEvent() throws PermissionNotFoundException, SecretStoringException, SecretLoadingException {
        // Given
        var event = mock(InboundPermissionAcceptEvent.class);
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permission = mock(Permission.class);
        var inboundDataSource = mock(InboundDataSource.class);

        // When
        when(event.permissionId()).thenReturn(permissionId);
        when(permissionService.findById(permissionId)).thenReturn(permission);
        when(dataSourceService.createInboundDataSource(permission)).thenReturn(inboundDataSource);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).createInboundDataSource(event);
        verify(permissionService).findById(permissionId);
        verify(dataSourceService).createInboundDataSource(permission);
        verify(permission).setDataSource(inboundDataSource);
        verify(dataSourceService).startDataSource(inboundDataSource);
    }

    @Test
    void testOnInboundPermissionAcceptEvent_missingMqttStreamingConfigIsHandled()
            throws PermissionNotFoundException, SecretStoringException, SecretLoadingException {
        var event = mock(InboundPermissionAcceptEvent.class);
        var permissionId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        var permission = mock(Permission.class);

        when(event.permissionId()).thenReturn(permissionId);
        when(permissionService.findById(permissionId)).thenReturn(permission);
        when(dataSourceService.createInboundDataSource(permission))
                .thenThrow(new MissingMqttStreamingConfigException(null));

        assertThatCode(() -> aiidaEventPublisher.publishEvent(event)).doesNotThrowAnyException();

        verify(aiidaEventListener).createInboundDataSource(event);
        verify(dataSourceService).createInboundDataSource(permission);
        verify(permission, never()).setDataSource(any());
        verify(dataSourceService, never()).startDataSource(any());
    }

    @Test
    void testOnInboundPermissionRevokeEvent() {
        // Given
        var event = mock(InboundPermissionRevokeEvent.class);
        var dataSourceId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        // When
        when(event.dataSourceId()).thenReturn(dataSourceId);
        aiidaEventPublisher.publishEvent(event);

        // Then
        verify(aiidaEventListener).deleteInboundDataSource(event);
        verify(provisioningService).stopPublisher(dataSourceId);
        verify(dataSourceService).deleteDataSource(dataSourceId);
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
