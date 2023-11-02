package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryPermimssionRequestRepositoryTest {
    @Test
    void givenNewRepository_whenSaveAndFindByPermissionId_thenPermissionRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId");

        // When
        repository.save(request);
        Optional<DkEnerginetCustomerPermissionRequest> foundRequest = repository.findByPermissionId("permissionId");

        // Then
        assertTrue(foundRequest.isPresent());
    }

    @Test
    void givenRepositoryWithRequests_whenFindByPermissionIdNonExistent_thenNoRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId");
        repository.save(request);

        // When
        Optional<DkEnerginetCustomerPermissionRequest> foundRequest = repository.findByPermissionId("nonExistentId");

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void givenRepositoryWithRequests_whenFindByPermissionIdNull_thenNoRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId", "dataNeedId");
        repository.save(request);

        // When
        // Then
        assertThrows(NullPointerException.class, () -> repository.findByPermissionId(null));
    }

    @Test
    void givenRepositoryWithMultipleRequests_whenFindByPermissionId_thenCorrectRequestsFound() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request1 = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId");
        var request2 = new SimplePermissionRequest("permissionId2", "connectionId2", "dataNeedId");
        repository.save(request1);
        repository.save(request2);

        // When
        Optional<DkEnerginetCustomerPermissionRequest> foundRequest1 = repository.findByPermissionId("permissionId1");

        // Then
        assertEquals("connectionId1", foundRequest1.orElseThrow().connectionId());
        assertEquals("dataNeedId", foundRequest1.get().dataNeedId());
    }

    @Test
    void removeByPermissionId_withNonExistentKey_returnsFalse() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId");
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
        var request = new SimplePermissionRequest("permissionId", "connectionId1", "dataNeedId");
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("permissionId");

        // Then
        assertTrue(found);
    }
}