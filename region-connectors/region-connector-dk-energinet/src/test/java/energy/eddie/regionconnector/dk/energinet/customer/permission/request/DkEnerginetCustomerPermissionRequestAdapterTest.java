package energy.eddie.regionconnector.dk.energinet.customer.permission.request;

import energy.eddie.api.v0.process.model.FutureStateException;
import energy.eddie.api.v0.process.model.PastStateException;
import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.dk.energinet.customer.api.DkEnerginetCustomerPermissionRequest;
import energy.eddie.regionconnector.dk.energinet.customer.permission.request.states.EnerginetCustomerMalformedState;
import energy.eddie.regionconnector.dk.energinet.enums.TimeSeriesAggregationEnum;
import io.javalin.http.Context;
import io.javalin.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.Objects;

import static energy.eddie.regionconnector.dk.energinet.customer.permission.request.EnerginetCustomerPermissionRequest.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DkEnerginetCustomerPermissionRequestAdapterTest {
    @Test
    void adapter_returnsPermissionId() {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        var permissionId = adapter.permissionId();

        // Then
        assertEquals("permissionId", permissionId);
    }

    @Test
    void adapter_returnsConnectionId() {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        var connectionId = adapter.connectionId();

        // Then
        assertEquals("connectionId1", connectionId);
    }

    @Test
    void adapter_returnsStart() {
        // Given
        ZonedDateTime start = ZonedDateTime.now();
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", start, null);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.start();

        // Then
        assertEquals(start, res);
    }

    @Test
    void adapter_returnsEnd() {
        // Given
        ZonedDateTime end = ZonedDateTime.now();
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, end);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        var res = adapter.end();

        // Then
        assertEquals(end, res);
    }

    @Test
    void adapter_returnsState() {
        // Given
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, null, state);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

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
        when(ctx.formParamAsClass(any(), eq(ZonedDateTime.class)))
                .thenReturn(new Validator<>(null, ZonedDateTime.class, END_KEY));
        when(ctx.formParamAsClass(any(), eq(String.class)))
                .thenReturn(new Validator<>("", String.class, REFRESH_TOKEN_KEY));
        when(ctx.formParamAsClass(any(), eq(String.class)))
                .thenReturn(new Validator<>("", String.class, METERING_POINT_KEY));
        when(ctx.formParamAsClass(any(), eq(TimeSeriesAggregationEnum.class)))
                .thenReturn(new Validator<>(null, TimeSeriesAggregationEnum.class, AGGREGATION_KEY));
        EnerginetCustomerMalformedState state = new EnerginetCustomerMalformedState(null, null);
        DkEnerginetCustomerPermissionRequest request = new EnerginetCustomerPermissionRequest("pid", "cid", ctx, null);
        PermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.changeState(state);

        // Then
        assertEquals(state, adapter.state());
    }

    @Test
    void validateCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.validate();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void sendToPermissionAdministratorCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.sendToPermissionAdministrator();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void receivedPermissionAdministratorResponseCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.receivedPermissionAdministratorResponse();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void terminateCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.terminate();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void acceptCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.accept();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void invalidCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.invalid();

        // Then
        assertTrue(decorator.didChange());
    }

    @Test
    void rejectedCallsDecorator() throws FutureStateException, PastStateException {
        // Given
        DkEnerginetCustomerPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        ChangingPermissionRequest decorator = new ChangingPermissionRequest(request);
        DkEnerginetCustomerPermissionRequestAdapter adapter = new DkEnerginetCustomerPermissionRequestAdapter(request, decorator);

        // When
        adapter.rejected();

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
        public void rejected() {
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
