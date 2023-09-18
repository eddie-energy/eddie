package energy.eddie.regionconnector.at.eda.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.at.api.AtPermissionRequest;
import energy.eddie.regionconnector.at.eda.SimplePermissionRequest;
import energy.eddie.regionconnector.at.eda.permission.request.states.AtInvalidPermissionRequestState;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class EdaPermissionRequestAdapterTest {

    @Test
    void adapter_returnsPermissionId() {
        // Given
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var permissionId = adapter.permissionId();

        // Then
        assertEquals("pid", permissionId);
    }

    @Test
    void adapter_returnsConnectionId() {
        // Given
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var connectionId = adapter.connectionId();

        // Then
        assertEquals("cid", connectionId);
    }

    @Test
    void adapter_returnsCmRequestId() {
        // Given
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var cmId = adapter.cmRequestId();

        // Then
        assertEquals("cmId", cmId);
    }

    @Test
    void adapter_returnsConversationId() {
        // Given
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var conversationId = adapter.conversationId();

        // Then
        assertEquals("conversationId", conversationId);
    }

    @Test
    void adapter_returnsState() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.state();

        // Then
        assertEquals(state, res);
    }

    @Test
    void adapter_changesState() {
        // Given
        CCMORequest ccmoRequest = mock(CCMORequest.class);
        AtPermissionRequest permissionRequest = new EdaPermissionRequest("cid", ccmoRequest, null);
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        PermissionRequest decorator = new ThrowingPermissionRequest(permissionRequest);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(permissionRequest, decorator);

        // When
        adapter.changeState(state);

        // Then
        assertEquals(state, adapter.state());
    }

    @Test
    void validateCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::validate);
    }

    @Test
    void sendToPermissionAdministratorCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponseCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminateCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::terminate);
    }

    @Test
    void acceptCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::accept);
    }

    @Test
    void invalidCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::invalid);
    }

    @Test
    void rejectedCallsDecorator() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        // Then
        assertThrows(IllegalStateException.class, adapter::rejected);
    }

    @Test
    void adapter_equalsReturnsTrueForPermissionRequest() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.equals(request);

        // Then
        assertTrue(res);
    }

    @Test
    void adapter_equalsReturnsFalse() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.equals(new Object());

        // Then
        assertFalse(res);
    }

    @Test
    void adapter_hashCodeIsEqualToPermissionRequestHashCode() {
        // Given
        AtInvalidPermissionRequestState state = new AtInvalidPermissionRequestState(null);
        AtPermissionRequest request = new SimplePermissionRequest("pid", "cid", "cmId", "conversationId", state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        EdaPermissionRequestAdapter adapter = new EdaPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.hashCode();

        // Then
        assertEquals(request.hashCode(), res);
    }

    private record ThrowingPermissionRequest(PermissionRequest request) implements PermissionRequest {

        @Override
        public String permissionId() {
            return request.permissionId();
        }

        @Override
        public String connectionId() {
            return request.connectionId();
        }

        @Override
        public PermissionRequestState state() {
            return request.state();
        }

        @Override
        public void changeState(PermissionRequestState state) {
            request.changeState(state);
        }

        @Override
        public void validate() {
            throw new IllegalStateException();
        }

        @Override
        public void sendToPermissionAdministrator() {
            throw new IllegalStateException();
        }

        @Override
        public void receivedPermissionAdministratorResponse() {
            throw new IllegalStateException();
        }

        @Override
        public void terminate() {
            throw new IllegalStateException();
        }

        @Override
        public void accept() {
            throw new IllegalStateException();
        }

        @Override
        public void invalid() {
            throw new IllegalStateException();
        }

        @Override
        public void rejected() {
            throw new IllegalStateException();
        }
    }
}