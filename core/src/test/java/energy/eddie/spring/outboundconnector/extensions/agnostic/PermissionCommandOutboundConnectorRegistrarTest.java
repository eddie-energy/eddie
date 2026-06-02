// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.agnostic;

import energy.eddie.api.agnostic.outbound.PermissionCommandOutboundConnector;
import energy.eddie.core.services.agnostic.PermissionCommandRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings("DataFlowIssue")
@ExtendWith(MockitoExtension.class)
class PermissionCommandOutboundConnectorRegistrarTest {
    @Mock
    private PermissionCommandOutboundConnector connector;
    @Mock
    private PermissionCommandRouter router;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new PermissionCommandOutboundConnectorRegistrar(null, router));
        assertThrows(NullPointerException.class,
                     () -> new PermissionCommandOutboundConnectorRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtRouter() {
        // Given

        // When
        new PermissionCommandOutboundConnectorRegistrar(Optional.empty(), router);

        // Then
        verifyNoInteractions(router);
    }

    @Test
    void givenProvider_registersAtRouter() {
        // Given

        // When
        new PermissionCommandOutboundConnectorRegistrar(Optional.of(connector), router);

        // Then
        verify(router).registerPermissionCommandConnector(any());
    }
}