// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.agnostic;

import energy.eddie.api.agnostic.opaque.RegionConnectorOpaqueEnvelopeService;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.agnostic.OpaqueEnvelopeRouter;
import energy.eddie.regionconnector.aiida.AiidaRegionConnectorMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpaqueEnvelopeRouterRegistrarTest {
    @Mock
    private RegionConnector regionConnector;

    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<RegionConnectorOpaqueEnvelopeService> emptyService = Optional.empty();
        Optional<OpaqueEnvelopeRouter> emptyRouter = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new OpaqueEnvelopeRouterRegistrar(null, emptyService, emptyRouter));
        assertThrows(NullPointerException.class,
                     () -> new OpaqueEnvelopeRouterRegistrar(regionConnector, null, emptyRouter));
        assertThrows(NullPointerException.class,
                     () -> new OpaqueEnvelopeRouterRegistrar(regionConnector, emptyService, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        when(regionConnector.getMetadata()).thenReturn(AiidaRegionConnectorMetadata.getInstance());
        var service = mock(RegionConnectorOpaqueEnvelopeService.class);
        var router = mock(OpaqueEnvelopeRouter.class);

        // When
        new OpaqueEnvelopeRouterRegistrar(regionConnector, Optional.of(service), Optional.of(router));

        // Then
        verify(router).registerOpaqueEnvelopeService("aiida", service);
    }
}