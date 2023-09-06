package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.regionconnector.at.api.AtPermissionRequestRepository;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.CreatedPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SavingPermissionRequestTest {

    @Test
    void savingPermissionRequest_returnsStateOfWrappedPermissionRequest() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CreatedPermissionRequestState createdState = new CreatedPermissionRequestState(null, null, null);
        var permissionRequest = new SimplePermissionRequest("permissionId", "connectionId", "rid", "cid", createdState);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        var state = savingPermissionRequest.state();

        // Then
        assertEquals(createdState, state);
    }

    @Test
    void savingPermissionRequest_changesStateOfWrappedPermissionRequest() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CreatedPermissionRequestState createdState = new CreatedPermissionRequestState(null, null, null);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        savingPermissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, savingPermissionRequest.state());
    }

    @Test
    void savingPermissionRequest_returnsCMRequestId() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        String cmRequestId = savingPermissionRequest.cmRequestId();

        // Then
        assertEquals("cmRequestId", cmRequestId);
    }

    @Test
    void savingPermissionRequest_returnsConversationId() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        String conversationId = savingPermissionRequest.conversationId();

        // Then
        assertEquals("messageId", conversationId);
    }

    @Test
    void savingPermissionRequest_returnsPermissionId() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        String permissionId = savingPermissionRequest.permissionId();

        // Then
        assertEquals("pid", permissionId);
    }

    @Test
    void savingPermissionRequest_returnsConnectionId() {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);

        // When
        String connectionId = savingPermissionRequest.connectionId();

        // Then
        assertEquals("connectionId", connectionId);
    }

    @Test
    void savingPermissionRequest_validateUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.validate();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_sendToPermissionAdministratorUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.sendToPermissionAdministrator();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_receivedPermissionAdministratorResponseUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.receivedPermissionAdministratorResponse();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_invalidUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.invalid();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_acceptUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.accept();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_rejectedUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.rejected();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

    @Test
    void savingPermissionRequest_terminateUpdatesSave() throws FutureStateException, PastStateException {
        // Given
        AtPermissionRequestRepository repo = new InMemoryPermissionRequestRepository();
        var permissionRequest = new SimplePermissionRequest("pid", "connectionId", "cmRequestId", "conversationId", null);
        SavingPermissionRequest savingPermissionRequest = new SavingPermissionRequest(permissionRequest, repo);
        repo.removeByPermissionId("pid");

        // When
        savingPermissionRequest.terminate();

        assertTrue(repo.findByPermissionId("pid").isPresent());
    }

}