// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.v1_12.outbound.MinMaxEnvelopeOutboundConnector;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;
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
class MinMaxEnvelopeOutboundConnectorRegistrarTest {
    @Mock
    private MinMaxEnvelopeOutboundConnector connector;
    @Mock
    private MinMaxEnvelopeRouter service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeOutboundConnectorRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeOutboundConnectorRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new MinMaxEnvelopeOutboundConnectorRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new MinMaxEnvelopeOutboundConnectorRegistrar(Optional.of(connector), service);

        // Then
        verify(service).registerMinMaxEnvelopeConnector(any());
    }
}