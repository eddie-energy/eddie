package energy.eddie.spring.regionconnector.extensions;

import energy.eddie.api.agnostic.process.model.events.PermissionEventRepository;
import energy.eddie.api.v0.RegionConnector;
import energy.eddie.api.v0.RegionConnectorMetadata;
import energy.eddie.core.services.PermissionEventService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.Mockito.*;

public class PermissionEventRepositoryTest {

    @Test
    void testSupplierPresent_registersRepository() {
        // Given
        PermissionEventService service = mock(PermissionEventService.class);
        RegionConnector regionConnector = mock(RegionConnector.class);
        RegionConnectorMetadata metadata = mock(RegionConnectorMetadata.class);
        PermissionEventRepository repository = mock(PermissionEventRepository.class);
        Supplier<PermissionEventRepository> supplier = mock(Supplier.class);

        when(regionConnector.getMetadata()).thenReturn(metadata);
        when(metadata.id()).thenReturn("at-eda");
        when(supplier.get()).thenReturn(repository);

        // When
        new PermissionEventRepositoryRegistrar(Optional.of(supplier), regionConnector, service);

        // Then
        verify(supplier).get();
        verify(service).registerPermissionEventRepository(repository, "at-eda");
    }

    @Test
    void testSupplierNotPresent_doesNotRegisterRepository() {
        // Given
        PermissionEventService service = mock(PermissionEventService.class);
        RegionConnector regionConnector = mock(RegionConnector.class);
        RegionConnectorMetadata metadata = mock(RegionConnectorMetadata.class);

        when(regionConnector.getMetadata()).thenReturn(metadata);
        when(metadata.id()).thenReturn("at-eda");

        // When
        new PermissionEventRepositoryRegistrar(Optional.empty(), regionConnector, service);

        // Then
        verify(service, never()).registerPermissionEventRepository(any(), anyString());
    }
}
