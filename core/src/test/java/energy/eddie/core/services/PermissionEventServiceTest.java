package energy.eddie.core.services;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PermissionEventServiceTest {
    @Test
    void testRegisterPermissionEventRepository_success() {
        // Given
        PermissionEventService service = new PermissionEventService();
        PermissionEventRepository permissionEventRepository = mock(PermissionEventRepository.class);
        String beanName = "at-eda";
        String countryCode = "AT";

        RegionConnectorRepository rcRepository = mock(RegionConnectorRepository.class);
        when(rcRepository.getBeanName()).thenReturn(beanName);

        try (MockedStatic<RegionConnectorRepository> mockRepository = mockStatic(RegionConnectorRepository.class)) {
            mockRepository.when(() -> RegionConnectorRepository.fromCountryCode(countryCode))
                    .thenReturn(Optional.of(rcRepository));
        }

        // When
        service.registerPermissionEventRepository(permissionEventRepository, beanName);
        PermissionEventRepository result = service.getPermissionEventRepositoryByCountryCode(countryCode);

        // Then
        assertSame(permissionEventRepository, result);
    }


    @Test
    void testGetPermissionEventRepositoryByCountryCode_invalidCountryCode() {
        // Given
        PermissionEventService service = new PermissionEventService();
        String countryCode = "XX";

        try (MockedStatic<RegionConnectorRepository> mockRepository = mockStatic(RegionConnectorRepository.class)) {
            mockRepository.when(() -> RegionConnectorRepository.fromCountryCode(countryCode))
                    .thenReturn(Optional.empty());
        }

        // When
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getPermissionEventRepositoryByCountryCode(countryCode));

        // Then
        assertTrue(ex.getMessage().contains("Invalid region connector country"));
    }

    @Test
    void testGetPermissionEventRepositoryByCountryCode_repositoryNotRegistered() {
        // Given
        PermissionEventService service = new PermissionEventService();
        String beanName = "at-eda";
        String countryCode = "AT";

        RegionConnectorRepository rcRepository = mock(RegionConnectorRepository.class);
        when(rcRepository.getBeanName()).thenReturn(beanName);

        try (MockedStatic<RegionConnectorRepository> mockRepository = mockStatic(RegionConnectorRepository.class)) {
            mockRepository.when(() -> RegionConnectorRepository.fromCountryCode(countryCode))
                    .thenReturn(Optional.of(rcRepository));
        }

        // When
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getPermissionEventRepositoryByCountryCode(countryCode));

        // Then
        assertTrue(ex.getMessage().contains("No repository found for bean name"));
    }
}
