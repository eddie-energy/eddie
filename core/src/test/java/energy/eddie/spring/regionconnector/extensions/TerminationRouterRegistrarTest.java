// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.v0.RegionConnector;
import energy.eddie.core.services.v0_82.TerminationRouter;
import energy.eddie.spring.regionconnector.extensions.v0_82.TerminationRouterRegistrar;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TerminationRouterRegistrarTest {
    @Test
    void givenNull_constructor_throws() {
        // Given
        var router = Optional.of(mock(TerminationRouter.class));
        var mockRegionConnector = mock(RegionConnector.class);

        // When, Then
        assertThrows(NullPointerException.class, () -> new TerminationRouterRegistrar(null, router));
        assertThrows(NullPointerException.class, () -> new TerminationRouterRegistrar(mockRegionConnector, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var router = mock(TerminationRouter.class);
        var mockRegionConnector = mock(RegionConnector.class);

        // When
        new TerminationRouterRegistrar(mockRegionConnector, Optional.of(router));

        // Then
        verify(router).registerRegionConnector(mockRegionConnector);
    }
}