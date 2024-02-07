package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.SentToPermissionAdministratorState;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InMemoryPermissionRequestRepositoryTest {
    @Test
    void givenNewRepository_whenSaveAndFindByPermissionId_thenPermissionRequestFound() {
        // Given
        InMemoryPermissionRequestRepository repository = new InMemoryPermissionRequestRepository();
        var request = new SimplePermissionRequest("permissionId", "connectionId");

        // When
        repository.save(request);
        Optional<EsPermissionRequest> foundRequest = repository.findByPermissionId("permissionId");

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
        Optional<EsPermissionRequest> foundRequest = repository.findByPermissionId("nonExistentId");

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
        Optional<EsPermissionRequest> foundRequest1 = repository.findByPermissionId("permissionId1");

        // Then
        assertEquals("connectionId1", foundRequest1.get().connectionId());
    }


    @Test
    void findAllAccepted_returnsOnlyAcceptedRequestsFound() {
        // Given
        var repository = new InMemoryPermissionRequestRepository();
        AcceptedState acceptedState = mock(AcceptedState.class);
        when(acceptedState.status()).thenReturn(PermissionProcessStatus.ACCEPTED);
        SentToPermissionAdministratorState sentToPermissionAdministratorState = mock(SentToPermissionAdministratorState.class);
        when(sentToPermissionAdministratorState.status()).thenReturn(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        var request1 = new SimplePermissionRequest("permissionId1", "connectionId1", acceptedState);
        var request2 = new SimplePermissionRequest("permissionId2", "connectionId2", acceptedState);
        var request3 = new SimplePermissionRequest("permissionId3", "connectionId3", sentToPermissionAdministratorState);
        repository.save(request1);
        repository.save(request2);
        repository.save(request3);

        // When
        var acceptedRequests = repository.findAllAccepted().toList();

        // Then
        assertEquals(2, acceptedRequests.size());
        assertThat(acceptedRequests).containsExactlyInAnyOrder(request1, request2);
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