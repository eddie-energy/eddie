// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PermissionEventServiceTest {

    @Mock
    private PermissionEventRepository permissionEventRepository;

    @Test
    void testRegisterPermissionEventRepository_success() {
        // Given
        PermissionEventService service = new PermissionEventService();
        String regionConnectorId = "at-eda";

        // When
        service.registerPermissionEventRepository(permissionEventRepository, regionConnectorId);
        Optional<PermissionEventRepository> result = service.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(permissionEventRepository, result.get());
    }

    @Test
    void testGetPermissionEventRepositoryByCountryCode_repositoryNotRegistered() {
        // Given
        PermissionEventService service = new PermissionEventService();
        String regionConnectorId = "at-eda";

        // When
        Optional<PermissionEventRepository> result = service.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId);

        // Then
        assertFalse(result.isPresent());
    }
}
