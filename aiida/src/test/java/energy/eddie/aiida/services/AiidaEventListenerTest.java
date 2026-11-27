// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.services;

import energy.eddie.aiida.dtos.events.DataSourceDeletionEvent;
import energy.eddie.aiida.errors.auth.InvalidUserException;
import energy.eddie.aiida.errors.auth.UnauthorizedException;
import energy.eddie.aiida.errors.permission.InboundDataSourceInUseException;
import energy.eddie.aiida.errors.permission.PermissionNotFoundException;
import energy.eddie.aiida.publisher.AiidaEventPublisher;
import energy.eddie.api.agnostic.process.model.PermissionStateTransitionException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.*;

@SpringBootTest(classes = {AiidaEventListener.class, AiidaEventPublisher.class})
class AiidaEventListenerTest {
    @Autowired
    private AiidaEventPublisher aiidaEventPublisher;
    @MockitoBean
    private PermissionService permissionService;
    @MockitoSpyBean
    @InjectMocks
    private AiidaEventListener aiidaEventListener;

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
