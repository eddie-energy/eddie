// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions.v1_04;

import energy.eddie.api.v1_04.outbound.NearRealTimeDataMarketDocumentOutboundConnectorV1_04;
import energy.eddie.core.services.v1_04.NearRealTimeDataMarketDocumentService;
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
class NearRealTImeDataOutboundRegistrarV1_04Test {
    @Mock
    private NearRealTimeDataMarketDocumentOutboundConnectorV1_04 rtdConnector;
    @Mock
    private NearRealTimeDataMarketDocumentService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(rtdConnector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_04(null, service));
        assertThrows(NullPointerException.class,
                     () -> new NearRealTimeDataOutboundRegistrarV1_04(optional, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_04(Optional.empty(), service);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new NearRealTimeDataOutboundRegistrarV1_04(Optional.of(rtdConnector), service);

        // Then
        verify(service).getNearRealTimeDataMarketDocumentStream();
        verify(rtdConnector).setNearRealTimeDataMarketDocumentStreamV1_04(any());
    }
}