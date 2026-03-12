// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.agnostic;

import energy.eddie.api.agnostic.outbound.OpaqueEnvelopeOutboundConnector;
import energy.eddie.core.services.agnostic.OpaqueEnvelopeRouter;
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
class OpaqueEnvelopeOutboundConnectorRegistrarTest {
    @Mock
    private OpaqueEnvelopeOutboundConnector connector;
    @Mock
    private OpaqueEnvelopeRouter service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new OpaqueEnvelopeOutboundConnectorRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new OpaqueEnvelopeOutboundConnectorRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new OpaqueEnvelopeOutboundConnectorRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new OpaqueEnvelopeOutboundConnectorRegistrar(Optional.of(connector), service);

        // Then
        verify(service).registerOpaqueEnvelopeConnector(any());
    }
}