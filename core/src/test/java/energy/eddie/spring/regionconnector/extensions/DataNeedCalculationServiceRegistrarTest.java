// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.core.services.DataNeedCalculationRouter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DataNeedCalculationServiceRegistrarTest {
    @Mock
    private DataNeedCalculationRouter router;
    @Mock
    private DataNeedCalculationService service;

    @Test
    void givenNull_constructor_throws() {
        // Given
        Optional<DataNeedCalculationService> empty = Optional.empty();

        // When, Then
        assertThrows(NullPointerException.class, () -> new DataNeedCalculationServiceRegistrar(null, router));
        assertThrows(NullPointerException.class, () -> new DataNeedCalculationServiceRegistrar(empty, null));
    }

    @Test
    void givenNoProvider_noRegistrationAtService() {
        // Given
        // When
        new DataNeedCalculationServiceRegistrar(Optional.empty(), router);

        // Then
        verifyNoInteractions(router);
    }

    @Test
    void givenProvider_registersAtService() {
        // Given
        // When
        new DataNeedCalculationServiceRegistrar(Optional.of(service), router);

        // Then
        verify(router).register(service);
    }
}