package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.v0.process.model.PermissionRequest;
import energy.eddie.api.v0.process.model.PermissionRequestState;
import energy.eddie.regionconnector.es.datadis.api.MeasurementType;
import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import energy.eddie.regionconnector.es.datadis.permission.request.state.MalformedState;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DatadisPermissionRequestAdapterTest {
    @Test
    void adapter_returnsPermissionId() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var permissionId = adapter.permissionId();

        assertEquals("permissionId", permissionId);
    }

    @Test
    void adapter_returnsConnectionId() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var connectionId = adapter.connectionId();

        assertEquals("connectionId1", connectionId);
    }

    @Test
    void adapter_returnsPermissionStart() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", start, null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.permissionStart();

        assertEquals(start, res);
    }

    @Test
    void adapter_returnsPermissionEnd() {
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, end);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.permissionEnd();

        assertEquals(end, res);
    }

    @Test
    void adapter_returnsRequestDateTo() {
        // Given
        ZonedDateTime end = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, end);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.requestDataTo();

        assertEquals(end, res);
    }

    @Test
    void adapter_returnsRequestDateFrom() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", start, null);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.requestDataFrom();

        assertEquals(start, res);
    }


    @Test
    void adapter_returnsLastPulledMeterReading() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = SimplePermissionRequest.fromLastPulledMeterReading(start);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.lastPulledMeterReading();

        assertTrue(res.isPresent());
        assertEquals(start, res.get());
    }

    @Test
    void adapter_setsLastPulledMeterReading() {
        ZonedDateTime start = ZonedDateTime.now(ZoneId.systemDefault());
        EsPermissionRequest request = new SimplePermissionRequest();
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);


        adapter.setLastPulledMeterReading(start);
        var res = adapter.lastPulledMeterReading();

        assertTrue(res.isPresent());
        assertEquals(start, res.get());
    }


    @Test
    void adapter_returnsNif() {
        String nif = "nif";
        EsPermissionRequest request = SimplePermissionRequest.fromNif(nif);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.nif();

        assertEquals(nif, res);
    }

    @Test
    void adapter_returnsDataNeedId() {
        String dataNeedId = "dataNeedId";
        EsPermissionRequest request = SimplePermissionRequest.fromDataNeedId(dataNeedId);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.dataNeedId();

        assertEquals(dataNeedId, res);
    }


    @Test
    void adapter_returnsMeteringPointId() {
        String meteringPointId = "meteringPointId";
        EsPermissionRequest request = SimplePermissionRequest.fromMetringPointId(meteringPointId);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.meteringPointId();

        assertEquals(meteringPointId, res);
    }


    @Test
    void adapter_returnsMeasurementType() {
        MeasurementType measurementType = MeasurementType.HOURLY;
        EsPermissionRequest request = SimplePermissionRequest.fromMeasurementType(measurementType);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.measurementType();

        assertEquals(measurementType, res);
    }


    @Test
    void adapter_returnsState() {
        MalformedState state = new MalformedState(null, null);
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1", null, null, state);
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        var res = adapter.state();

        assertEquals(state, res);
    }

    @Test
    void adapter_changesState() {
        MalformedState state = new MalformedState(null, null);
        EsPermissionRequest request = new SimplePermissionRequest("pid", "cid");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        adapter.changeState(state);

        assertEquals(state, adapter.state());
    }

    @Test
    void validateCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::validate);
    }

    @Test
    void sendToPermissionAdministratorCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::sendToPermissionAdministrator);
    }

    @Test
    void receivedPermissionAdministratorResponseCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::receivedPermissionAdministratorResponse);
    }

    @Test
    void terminateCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::terminate);
    }

    @Test
    void acceptCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::accept);
    }

    @Test
    void invalidCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::invalid);
    }

    @Test
    void rejectedCallsDecorator() {
        EsPermissionRequest request = new SimplePermissionRequest("permissionId", "connectionId1");
        PermissionRequest decorator = new ThrowingPermissionRequest(request);
        DatadisPermissionRequestAdapter adapter = new DatadisPermissionRequestAdapter(request, decorator);

        assertThrows(IllegalStateException.class, adapter::reject);
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
        public void reject() {
            throw new IllegalStateException();
        }
    }
}