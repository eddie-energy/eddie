package energy.eddie.regionconnector.es.datadis.permission.request.state;

import energy.eddie.regionconnector.es.datadis.permission.request.api.EsPermissionRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class UnableToSendStateTest {
    @Test
    void reason_returnsGivenThrowable() {
        var throwable = new Throwable("test");

        UnableToSendState unableToSendState = new UnableToSendState(mock(EsPermissionRequest.class), throwable);

        assertEquals(throwable, unableToSendState.reason());
    }

    @Test
    void testToString() {
        var throwable = new Throwable("test");
        var expected = "UnableToSendState{reason=" + throwable + "}";
        UnableToSendState unableToSendState = new UnableToSendState(mock(EsPermissionRequest.class), throwable);

        assertEquals(expected, unableToSendState.toString());
    }
}