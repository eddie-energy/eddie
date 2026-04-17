// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.v1_12.EnergySharingReferenceDataMarketDocumentProvider;
import energy.eddie.core.services.v1_12.EnergySharingReferenceDataMarketDocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnergySharingReferenceDataMarketDocumentServiceRegistrarTest {

    @Mock
    private EnergySharingReferenceDataMarketDocumentService service;
    @Mock
    private ObjectProvider<EnergySharingReferenceDataMarketDocumentProvider> objectProvider;

    @Test
    void givenProvider_registers() {
        // Given
        doNothing().when(objectProvider).ifAvailable(any());

        // When
        new EnergySharingReferenceDataMarketDocumentServiceRegistrar(objectProvider, service);

        // Then
        verify(objectProvider).ifAvailable(any());
    }
}