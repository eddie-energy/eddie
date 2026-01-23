// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.outboundconnector.extensions;

import energy.eddie.api.agnostic.outbound.RawDataOutboundConnector;
import energy.eddie.core.services.RawDataService;
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
class RawDataOutboundRegistrarTest {
    @Mock
    private RawDataOutboundConnector connector;
    @Mock
    private RawDataService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        var optional = Optional.of(connector);
        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new RawDataOutboundRegistrar(null, service, true));
        assertThrows(NullPointerException.class,
                     () -> new RawDataOutboundRegistrar(optional, null, true));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given

        // When
        new RawDataOutboundRegistrar(Optional.empty(), service, true);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenNoProvider_withDisabledRawData_doesNothing() {
        // Given

        // When
        new RawDataOutboundRegistrar(Optional.of(connector), service, false);

        // Then
        verifyNoInteractions(service);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given

        // When
        new RawDataOutboundRegistrar(Optional.of(connector), service, true);

        // Then
        verify(service).getRawDataStream();
        verify(connector).setRawDataStream(any());
    }
}