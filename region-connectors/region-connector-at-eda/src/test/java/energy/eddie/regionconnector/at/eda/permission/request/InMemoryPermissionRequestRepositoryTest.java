package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
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
        Optional<AtPermissionRequest> foundRequest = repository.findByPermissionId("permissionId");

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
        Optional<AtPermissionRequest> foundRequest = repository.findByPermissionId("nonExistentId");

        // Then
        assertFalse(foundRequest.isPresent());
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
        Optional<AtPermissionRequest> foundRequest1 = repository.findByPermissionId("permissionId1");

        // Then
        assertEquals("connectionId1", foundRequest1.get().connectionId());
    }

    // TODO: add tests
    @Test
    void findByConversationIdAndCmRequestId_returnsEmptyOptional_ifIdsDoNotMatch() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("asdf", "hjkl");

        // Then
        assertTrue(foundRequest.isEmpty());
    }

    @Test
    void findByConversationIdAndCmRequestId_returnsRequest_ifConversationIdMatches() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("cid1", "hjkl");

        // Then
        assertEquals(request, foundRequest.get());
    }

    @Test
    void findByConversationIdAndCmRequestId_returnsRequest_ifCmRequestIdMatches() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        Optional<AtPermissionRequest> foundRequest = repository.findByConversationIdOrCMRequestId("asdf", "rid1");

        // Then
        assertEquals(request, foundRequest.get());
    }

    @Test
    void removeByPermissionId_withNonExistentKey_returnsFalse() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId1", "connectionId1", "dataNeedId", "rid1", "cid1", null);
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
        var request = new SimplePermissionRequest("permissionId", "connectionId1", "dataNeedId", "rid1", "cid1", null);
        repository.save(request);

        // When
        boolean found = repository.removeByPermissionId("permissionId");

        // Then
        assertTrue(found);
    }
}