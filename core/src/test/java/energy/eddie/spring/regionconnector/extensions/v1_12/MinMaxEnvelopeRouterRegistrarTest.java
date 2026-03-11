// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v1_12.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinMaxEnvelopeRouterRegistrarTest {
    @Mock
    private RegionConnector regionConnector;

    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<RegionConnectorMinMaxEnvelopeService> emptyService = Optional.empty();
        Optional<MinMaxEnvelopeRouter> emptyRouter = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeRouterRegistrar(null, emptyService, emptyRouter));
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeRouterRegistrar(regionConnector, null, emptyRouter));
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeRouterRegistrar(regionConnector, emptyService, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        when(regionConnector.getMetadata()).thenReturn(AiidaRegionConnectorMetadata.getInstance());

        var service = mock(RegionConnectorMinMaxEnvelopeService.class);
        var router = mock(MinMaxEnvelopeRouter.class);

        // When
        new MinMaxEnvelopeRouterRegistrar(regionConnector, Optional.of(service), Optional.of(router));

        // Then
        verify(router).registerMinMaxEnvelopeService("aiida", service);
    }
}