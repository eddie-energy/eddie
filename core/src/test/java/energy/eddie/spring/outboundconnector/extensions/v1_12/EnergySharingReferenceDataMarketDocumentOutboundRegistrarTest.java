// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.v1_12.outbound.EnergySharingReferenceDataMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_12.EnergySharingReferenceDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EnergySharingReferenceDataMarketDocumentOutboundRegistrarTest {
    @Mock
    private EnergySharingReferenceDataMarketDocumentOutboundConnector connector;
    @Mock
    private EnergySharingReferenceDataMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new EnergySharingReferenceDataMarketDocumentOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new EnergySharingReferenceDataMarketDocumentOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new EnergySharingReferenceDataMarketDocumentOutboundRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new EnergySharingReferenceDataMarketDocumentOutboundRegistrar(Optional.of(connector), service);

        // Then
        verify(service).getEnergySharingReferenceDataMarketDocumentStream();
        verify(connector).setEnergySharingReferenceDataMarketDocumentStream(any());
    }
}