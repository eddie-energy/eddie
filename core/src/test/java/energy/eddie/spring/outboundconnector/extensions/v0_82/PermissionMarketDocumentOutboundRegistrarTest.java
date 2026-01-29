// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v0_82;

import energy.eddie.api.v0_82.outbound.PermissionMarketDocumentOutboundConnector;
import energy.eddie.core.services.v0_82.PermissionMarketDocumentService;
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
class PermissionMarketDocumentOutboundRegistrarTest {
    @Mock
    private PermissionMarketDocumentOutboundConnector connector;
    @Mock
    private PermissionMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new PermissionMarketDocumentOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new PermissionMarketDocumentOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new PermissionMarketDocumentOutboundRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new PermissionMarketDocumentOutboundRegistrar(Optional.of(connector), service);

        // Then
        verify(service).getPermissionMarketDocumentStream();
        verify(connector).setPermissionMarketDocumentStream(any());
    }
}