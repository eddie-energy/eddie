// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.PermissionEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PermissionEventRepositoryTest {

    @Mock
    private PermissionEventService permissionEventService;

    @Mock
    private PermissionEventRepository permissionEventRepository;

    @Mock
    private RegionConnector regionConnector;

    @Mock
    private RegionConnectorMetadata metadata;

    @Test
    void testSupplierPresent_registersRepository() {
        // Given
        when(regionConnector.getMetadata()).thenReturn(metadata);
        when(metadata.id()).thenReturn("at-eda");
        Supplier<PermissionEventRepository> supplier = () -> permissionEventRepository;

        // When
        new PermissionEventRepositoryRegistrar(Optional.of(supplier), regionConnector, permissionEventService);

        // Then
        verify(permissionEventService).registerPermissionEventRepository(permissionEventRepository, "at-eda");
    }

    @Test
    void testSupplierNotPresent_doesNotRegisterRepository() {
        // Given
        when(regionConnector.getMetadata()).thenReturn(metadata);
        when(metadata.id()).thenReturn("at-eda");

        // When
        new PermissionEventRepositoryRegistrar(Optional.empty(), regionConnector, permissionEventService);

        // Then
        verify(permissionEventService, never()).registerPermissionEventRepository(any(), anyString());
    }
}
