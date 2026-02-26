// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_12;

import energy.eddie.api.v1_12.outbound.AcknowledgementMarketDocumentOutboundConnector;
import energy.eddie.core.services.v1_12.AcknowledgementMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SuppressWarnings({"DataFlowIssue", "java:S114"})
@ExtendWith(MockitoExtension.class)
class AcknowledgementOutboundRegistrarTest {
    @Mock
    private AcknowledgementMarketDocumentOutboundConnector ackConnector;
    @Mock
    private AcknowledgementMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(ackConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new AcknowledgementOutboundRegistrar(null, service));
        assertThrows(NullPointerException.class,
                     () -> new AcknowledgementOutboundRegistrar(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new AcknowledgementOutboundRegistrar(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new AcknowledgementOutboundRegistrar(Optional.of(ackConnector), service);

        // Then
        verify(service).getAcknowledgementMarketDocumentStream();
        verify(ackConnector).setAcknowledgementMarketDocumentStream(any());
    }
}