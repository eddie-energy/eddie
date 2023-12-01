package energy.eddie.regionconnector.fr.enedis.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.api.v0.process.model.StateTransitionException;
import energy.eddie.api.v0.process.model.TimeframedPermissionRequest;
import energy.eddie.regionconnector.fr.enedis.permission.request.states.FrEnedisMalformedState;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import static energy.eddie.regionconnector.fr.enedis.permission.request.EnedisPermissionRequest.START_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TimeFramedPermissionRequestAdapterTest {
    @Test
    void adapter_returnsPermissionId() {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        var permissionId = adapter.permissionId();

        // Then
        assertEquals("permissionId", permissionId);
    }

    @Test
    void adapter_returnsConnectionId() {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        var connectionId = adapter.connectionId();

        // Then
        assertEquals("connectionId1", connectionId);
    }

    @Test
    void adapter_returnsStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", start, null);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.start();

        // Then
        assertEquals(start, res);
    }

    @Test
    void adapter_returnsEnd() {
        // Given
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, end);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.end();

        // Then
        assertEquals(end, res);
    }

    @Test
    void adapter_returnsState() {
        // Given
        FrEnedisMalformedState state = new FrEnedisMalformedState(null, null);
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", "dataNeedId", null, null, state);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.state();

        // Then
        assertEquals(state, res);
    }

    @Test
    void adapter_changesState() {
        // Given
        Context ctx = mock(Context.class);
        when(ctx.formParamAsClass(any(), eq(ZonedDateTime.class)))
                .thenReturn(new Validator<>(null, ZonedDateTime.class, START_KEY));
        FrEnedisMalformedState state = new FrEnedisMalformedState(null, null);
        TimeframedPermissionRequest request = new EnedisPermissionRequest("pid", "cid", ctx, null);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.changeState(state);

        // Then
        assertEquals(state, adapter.state());
    }

    @Test
    void validateCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.validate();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void sendToPermissionAdministratorCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.sendToPermissionAdministrator();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void receivedPermissionAdministratorResponseCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.receivedPermissionAdministratorResponse();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void terminateCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.terminate();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void acceptCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.accept();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void invalidCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.invalid();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void rejectedCallsDecorator() throws StateTransitionException {
        // Given
        TimeframedPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        TimeFramedPermissionRequestAdapter adapter = new TimeFramedPermissionRequestAdapter(request, decorator);

        // When
        adapter.reject();

        // Then
        assertTrue(decorator.didChange());
    }

    private static final class ChangingPermissionRequest implements PermissionRequest {
        private final PermissionRequest request;
        private boolean change = false;

        private ChangingPermissionRequest(PermissionRequest request) {
            this.request = request;
        }

        @Override
        public String permissionId() {
            return request.permissionId();
        }

        @Override
        public String connectionId() {
            return request.connectionId();
        }

        @Override
        public String dataNeedId() {
            return request.dataNeedId();
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
            change = true;
        }

        @Override
        public void sendToPermissionAdministrator() {
            change = true;
        }

        @Override
        public void receivedPermissionAdministratorResponse() {
            change = true;
        }

        @Override
        public void terminate() {
            change = true;
        }

        @Override
        public void accept() {
            change = true;
        }

        @Override
        public void invalid() {
            change = true;
        }

        @Override
        public void reject() {
            change = true;
        }

        public boolean didChange() {
            return change;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (ChangingPermissionRequest) obj;
            return Objects.equals(this.request, that.request);
        }

        @Override
        public int hashCode() {
            return Objects.hash(request);
        }

        @Override
        public String toString() {
            return "ChaninginPermissionRequest[" +
                    "request=" + request + ']';
        }
    }
}