package energy.eddie.regionconnector.shared.permission.requests.extensions;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import energy.eddie.regionconnector.shared.permission.requests.decorators.SimplePermissionRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SavingExtensionTest {
    @Test
    @SuppressWarnings("unchecked")
    void extension_savesPermissionRequest() {
        // Given
        PermissionRequestRepository<PermissionRequest> repository = mock(PermissionRequestRepository.class);
        SavingExtension<PermissionRequest> extension = new SavingExtension<>(repository);
        PermissionRequest permissionRequest = new SimplePermissionRequest("pid", "cid", null, "dnid");

        // When
        extension.accept(permissionRequest);

        // Then
        verify(repository).save(permissionRequest);
    }
}