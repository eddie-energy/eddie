// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions.v1_12;

import energy.eddie.api.v1_12.outbound.RegionConnectorMinMaxEnvelopeService;
import energy.eddie.core.services.v1_12.MinMaxEnvelopeRouter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MinMaxEnvelopeRouterRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<RegionConnectorMinMaxEnvelopeService> emptyService = Optional.empty();
        Optional<MinMaxEnvelopeRouter> emptyRouter = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new MinMaxEnvelopeRouterRegistrar(null, emptyService, emptyRouter));
        assertThrows(NullPointerException.class, () -> new MinMaxEnvelopeRouterRegistrar("test", null, emptyRouter));
        assertThrows(NullPointerException.class, () -> new MinMaxEnvelopeRouterRegistrar("test", emptyService, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var service = mock(RegionConnectorMinMaxEnvelopeService.class);
        var router = mock(MinMaxEnvelopeRouter.class);

        // When
        new MinMaxEnvelopeRouterRegistrar("test", Optional.of(service), Optional.of(router));

        // Then
        verify(router).registerMinMaxEnvelopeService("test", service);
    }
}