package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.EdaAdapter;
import energy.eddie.regionconnector.at.eda.permission.request.states.*;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EdaPermissionRequestTest {

    @Test
    void edaPermissionRequest_hasCreatedStateAsInitialState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("cid", ccmoRequest, null);

        // When
        var state = permissionRequest.state();

        // Then
        assertEquals(AtCreatedPermissionRequestState.class, state.getClass());
    }

    @Test
    void edaPermissionRequest_changesState() {
        // Given
        AtCreatedPermissionRequestState createdState = new AtCreatedPermissionRequestState(null, null, null);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        PermissionRequest permissionRequest = new EdaPermissionRequest("cid", ccmoRequest, null);

        // When
        permissionRequest.changeState(createdState);

        // Then
        assertEquals(createdState, permissionRequest.state());
    }

    @Test
    void edaPermissionRequest_returnsCMRequestId() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);

        // When
        String cmRequestId = permissionRequest.cmRequestId();

        // Then
        assertEquals("cmRequestId", cmRequestId);
    }

    @Test
    void messagingPermissionRequest_returnsConversationId() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        var permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);

        // When
        String conversationId = permissionRequest.conversationId();

        // Then
        assertEquals("messageId", conversationId);
    }

    @Test
    void equalEdaPermissionRequests_returnTrue() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertTrue(res);
    }

    @Test
    void differentObjectEdaPermissionRequestsEquals_returnsFalse() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);

        // When
        boolean res = permissionRequest.equals(new Object());

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentConnectionId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("cid", "pid", ccmoRequest, null);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentPermissionId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid1", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid2", ccmoRequest, null);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentCMRequestId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest1 = mock(CCMORequest.class);
        when(ccmoRequest1.cmRequestId()).thenReturn("cmRequestId1");
        when(ccmoRequest1.messageId()).thenReturn("messageId");
        CCMORequest ccmoRequest2 = mock(CCMORequest.class);
        when(ccmoRequest2.cmRequestId()).thenReturn("cmRequestId2");
        when(ccmoRequest2.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest1, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest2, null);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentConversationId_areNotEqual() {
        // Given
        CCMORequest ccmoRequest1 = mock(CCMORequest.class);
        when(ccmoRequest1.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest1.messageId()).thenReturn("messageId1");
        CCMORequest ccmoRequest2 = mock(CCMORequest.class);
        when(ccmoRequest2.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest2.messageId()).thenReturn("messageId2");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest1, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest2, null);

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void edaPermissionRequests_withDifferentStates_areNotEqual() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid1", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid2", ccmoRequest, null);
        permissionRequest2.changeState(new AtInvalidPermissionRequestState(permissionRequest2));

        // When
        boolean res = permissionRequest1.equals(permissionRequest2);

        // Then
        assertFalse(res);
    }

    @Test
    void equalEdaPermissionRequests_haveSameHash() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);

        // When
        int res1 = permissionRequest1.hashCode();
        int res2 = permissionRequest2.hashCode();

        // Then
        assertEquals(res1, res2);
    }

    @Test
    void equalEdaPermissionRequests_haveDifferentHash() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest1 = new EdaPermissionRequest("connectionId", "pid", ccmoRequest, null);
        PermissionRequest permissionRequest2 = new EdaPermissionRequest("cid", "pid2", ccmoRequest, null);

        // When
        int res1 = permissionRequest1.hashCode();
        int res2 = permissionRequest2.hashCode();

        // Then
        assertNotEquals(res1, res2);
    }

    @Test
    void validatedTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, null);

        // When
        permissionRequest.validate();

        // Then
        assertEquals(AtValidatedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void sendToPermissionAdministratorTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.validate();

        // When
        permissionRequest.sendToPermissionAdministrator();


        // Then
        assertEquals(AtPendingAcknowledgmentPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void receivedPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();

        // When
        permissionRequest.receivedPermissionAdministratorResponse();

        // Then
        assertEquals(AtSentToPermissionAdministratorPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void acceptPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.accept();

        // Then
        assertEquals(AtAcceptedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void invalidPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.invalid();

        // Then
        assertEquals(AtInvalidPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void rejectPermissionAdministratorResponseTransitionsEdaPermissionRequest() throws FutureStateException, PastStateException {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        PermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.validate();
        permissionRequest.sendToPermissionAdministrator();
        permissionRequest.receivedPermissionAdministratorResponse();

        // When
        permissionRequest.rejected();

        // Then
        assertEquals(AtRejectedPermissionRequestState.class, permissionRequest.state().getClass());
    }

    @Test
    void terminatePermissionAdministratorResponseTransitionsEdaPermissionRequest() {
        // Given
        EdaAdapter edaAdapter = mock(EdaAdapter.class);
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        when(ccmoRequest.cmRequestId()).thenReturn("cmRequestId");
        when(ccmoRequest.messageId()).thenReturn("messageId");
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("connectionId", ccmoRequest, edaAdapter);
        permissionRequest.changeState(new AtAcceptedPermissionRequestState(permissionRequest));

        // When
        // Then
        assertThrows(IllegalStateException.class, permissionRequest::terminate);
    }

}