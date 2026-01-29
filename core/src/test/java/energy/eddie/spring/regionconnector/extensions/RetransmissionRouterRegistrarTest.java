// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.retransmission.RegionConnectorRetransmissionService;
import energy.eddie.core.services.CoreRetransmissionRouter;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RetransmissionRouterRegistrarTest {

    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<RegionConnectorRetransmissionService> emptyService = Optional.empty();
        Optional<CoreRetransmissionRouter> emptyRouter = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class,
                     () -> new RetransmissionRouterRegistrar(null, emptyService, emptyRouter));
        assertThrows(NullPointerException.class, () -> new RetransmissionRouterRegistrar("test", null, emptyRouter));
        assertThrows(NullPointerException.class, () -> new RetransmissionRouterRegistrar("test", emptyService, null));
    }

    @Test
    void givenRegionConnector_registersAtService() {
        // Given
        var service = mock(RegionConnectorRetransmissionService.class);
        var router = mock(CoreRetransmissionRouter.class);
        String name = "rc";

        // When
        new RetransmissionRouterRegistrar(name, Optional.of(service), Optional.of(router));

        // Then
        verify(router).registerRetransmissionService(name, service);
    }
}
