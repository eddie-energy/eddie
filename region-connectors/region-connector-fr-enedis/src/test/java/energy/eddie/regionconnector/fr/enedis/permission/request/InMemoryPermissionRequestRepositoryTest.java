package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPermissionRequestRepositoryTest {
    @Test
    void givenNewRepository_whenSaveAndFindByPermissionId_thenPermissionRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");

        // When
        repository.save(request);
        Optional<TimeframedPermissionRequest> foundRequest = repository.findByPermissionId("permissionId");

        // Then
        assertTrue(foundRequest.isPresent());
    }

    @Test
    void givenRepositoryWithRequests_whenFindByPermissionIdNonExistent_thenNoRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");
        repository.save(request);

        // When
        Optional<TimeframedPermissionRequest> foundRequest = repository.findByPermissionId("nonExistentId");

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void givenRepositoryWithRequests_whenFindByPermissionIdNull_thenNoRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");
        repository.save(request);

        // When
        Optional<TimeframedPermissionRequest> foundRequest = repository.findByPermissionId(null);

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void givenRepositoryWithMultipleRequests_whenFindByPermissionId_thenCorrectRequestsFound() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request1 = new SimplePermissionRequest("permissionId1", "connectionId1");
        var request2 = new SimplePermissionRequest("permissionId2", "connectionId2");
        repository.save(request1);
        repository.save(request2);

        // When
        Optional<TimeframedPermissionRequest> foundRequest1 = repository.findByPermissionId("permissionId1");

        // Then
        assertEquals("connectionId1", foundRequest1.get().connectionId());
    }

    @Test
    void removeByPermissionId_withNonExistentKey_returnsFalse() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1");
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("asdf");

        // Then
        assertFalse(found);
    }

    @Test
    void removeByPermissionId_withExistingKey_returnsTrue() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId1");
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("permissionId");

        // Then
        assertTrue(found);
    }
}