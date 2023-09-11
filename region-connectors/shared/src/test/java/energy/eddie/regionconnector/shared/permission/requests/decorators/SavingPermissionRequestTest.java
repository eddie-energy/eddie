package energy.eddie.regionconnector.shared.permission.requests.decorators;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavingPermissionRequestTest {
    @Test
    void savingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        var state = savingPermissionRequest.state();

        // Then
        assertEquals(createdState, state);
    }

    @Test
    void savingPermissionRequest_changesStateOfWrappedPermissionRequest() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, savingPermissionRequest.state());
    }

    @Test
    void savingPermissionRequest_returnsPermissionId() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        String permissionId = savingPermissionRequest.permissionId();

        // Then
        assertEquals("permissionId", permissionId);
    }

    @Test
    void savingPermissionRequest_returnsConnectionId() {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        String connectionId = savingPermissionRequest.connectionId();

        // Then
        assertEquals("connectionId", connectionId);
    }

    @Test
    void savingPermissionRequest_validateUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.validate();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_sendToPermissionAdministratorUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.sendToPermissionAdministrator();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_receivedPermissionAdministratorResponseUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.receivedPermissionAdministratorResponse();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_invalidUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.invalid();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_acceptUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.accept();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_rejectedUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.rejected();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    @Test
    void savingPermissionRequest_terminateUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        PermissionRequestRepository<PermissionRequest> repo = new SimplePermissionRequestRepository();
        SimpleState createdState = new SimpleState();
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", createdState);
        SavingPermissionRequest<PermissionRequest> savingPermissionRequest = new SavingPermissionRequest<>(permissionRequest, repo);

        // When
        savingPermissionRequest.terminate();

        assertTrue(repo.findByPermissionId("permissionId").isPresent());
    }

    private final static class SimplePermissionRequestRepository implements PermissionRequestRepository<PermissionRequest> {
        private PermissionRequest permissionRequest = null;

        @Override
        public void save(PermissionRequest request) {
            permissionRequest = request;
        }

        @Override
        public Optional<PermissionRequest> findByPermissionId(String permissionId) {
            return Optional.ofNullable(permissionRequest)
                    .filter(r -> r.permissionId().equals(permissionId));
        }

        @Override
        public boolean removeByPermissionId(String permissionId) {
            if (findByPermissionId(permissionId).isPresent()) {
                permissionRequest = null;
                return true;
            }
            return false;
        }
    }
}