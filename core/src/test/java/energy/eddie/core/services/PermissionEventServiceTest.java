package energy.eddie.core.services;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        PermissionEventRepository result = service.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId);

        // Then
        assertSame(permissionEventRepository, result);
    }

    @Test
    void testGetPermissionEventRepositoryByCountryCode_repositoryNotRegistered() {
        // Given
        PermissionEventService service = new PermissionEventService();
        String regionConnectorId = "at-eda";

        // When
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getPermissionEventRepositoryByRegionConnectorId(regionConnectorId));

        // Then
        assertTrue(ex.getMessage().contains("No repository found for region connector"));
    }
}
