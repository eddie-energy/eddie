package energy.eddie.regionconnector.aiida;

import energy.eddie.regionconnector.aiida.api.AiidaPermissionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAiidaPermissionRequestRepositoryTest {
    @Test
    void givenNewRepository_whenSaveAndFindByPermissionId_thenPermissionRequestFound() {
        var now = Instant.now();
        InMemoryAiidaPermissionRequestRepository repository = new InMemoryAiidaPermissionRequestRepository();
        String permissionId = "permissionId";
        var request = new AiidaPermissionRequest(permissionId, "connectionId",
                "dataNeedId", "foo", now, now, null);

        // When
        repository.save(request);
        Optional<AiidaPermissionRequest> foundRequest = repository.findByPermissionId(permissionId);

        // Then
        assertTrue(foundRequest.isPresent());
    }

    @Test
    void givenNewRepository_whenFindByPermissionId_thenPermissionRequestNotFound() {
        // Given
        InMemoryAiidaPermissionRequestRepository repository = new InMemoryAiidaPermissionRequestRepository();

        // When
        Optional<AiidaPermissionRequest> foundRequest = repository.findByPermissionId("NonExisting");

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void givenNonExistingPermissionId_removeByPermissionId_returnsFalse() {
        // Given
        InMemoryAiidaPermissionRequestRepository repository = new InMemoryAiidaPermissionRequestRepository();

        // When
        boolean removed = repository.removeByPermissionId("NonExisting");

        // Then
        assertFalse(removed);
    }

    @Test
    void givenExistingPermissionId_removeByPermissionId_returnsTrue() {
        // Given
        var now = Instant.now();
        InMemoryAiidaPermissionRequestRepository repository = new InMemoryAiidaPermissionRequestRepository();
        String permissionId = "permissionId";
        var request = new AiidaPermissionRequest(permissionId, "connectionId",
                "dataNeedId", "foo", now, now, null);
        repository.save(request);

        // When
        boolean removed = repository.removeByPermissionId(permissionId);

        // Then
        assertTrue(removed);
    }
}