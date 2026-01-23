// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_04;

import energy.eddie.api.v1_04.ValidatedHistoricalDataMarketDocumentProvider;
import energy.eddie.core.services.v1_04.ValidatedHistoricalDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidatedHistoricalDataMarketDocumentServiceRegistrarTest {
    @Mock
    private ValidatedHistoricalDataMarketDocumentService service;
    @Mock
    private ObjectProvider<ValidatedHistoricalDataMarketDocumentProvider> objectProvider;

    @Test
    void givenProvider_registers() {
        // Given
        doNothing().when(objectProvider).ifAvailable(any());

        // When
        new ValidatedHistoricalDataMarketDocumentServiceRegistrar(objectProvider, service);

        // Then
        verify(objectProvider).ifAvailable(any());
    }
}