package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class InvalidStateTest {

    @Test
    void reason_returnsGivenThrowable() {
        var throwable = new Throwable("test");

        InvalidState invalidState = new InvalidState(mock(EsPermissionRequest.class), throwable);

        assertEquals(throwable, invalidState.reason());
    }

    @Test
    void testToString() {
        var throwable = new Throwable("test");
        var expected = "InvalidState{reason=" + throwable + "}";
        InvalidState invalidState = new InvalidState(mock(EsPermissionRequest.class), throwable);

        assertEquals(expected, invalidState.toString());
    }
}