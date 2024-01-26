package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class MalformedStateTest {

    @Test
    void reason_returnsGivenThrowable() {
        var throwable = new Throwable("test");

        MalformedState malformedState = new MalformedState(mock(EsPermissionRequest.class), throwable);

        assertEquals(throwable, malformedState.reason());
    }

    @Test
    void testToString() {
        var throwable = new Throwable("test");
        var expected = "MalformedPermissionRequestState{reason=" + throwable + "}";
        MalformedState malformedState = new MalformedState(mock(EsPermissionRequest.class), throwable);

        assertEquals(expected, malformedState.toString());
    }
}