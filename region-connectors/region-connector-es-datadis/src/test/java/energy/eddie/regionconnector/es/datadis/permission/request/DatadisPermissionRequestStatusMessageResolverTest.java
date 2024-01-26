package energy.eddie.regionconnector.es.datadis.permission.request;

import energy.eddie.api.agnostic.process.model.PermissionRequestState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.AcceptedState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.InvalidState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.MalformedState;
import energy.eddie.regionconnector.es.datadis.permission.request.state.UnableToSendState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatadisPermissionRequestStatusMessageResolverTest {

    private static Stream<Arguments> permissionStates() {
        String message = "message";

        var invalidState = mock(InvalidState.class);
        when(invalidState.reason()).thenReturn(new Throwable(message));
        var malformedState = mock(MalformedState.class);
        when(malformedState.reason()).thenReturn(new Throwable(message));
        var unableToSendState = mock(UnableToSendState.class);
        when(unableToSendState.reason()).thenReturn(new Throwable(message));
        var acceptedState = mock(AcceptedState.class);

        return Stream.of(
                Arguments.of(invalidState, message),
                Arguments.of(malformedState, message),
                Arguments.of(unableToSendState, message),
                Arguments.of(acceptedState, null)
        );
    }

    @ParameterizedTest
    @MethodSource("permissionStates")
    void getStatusMessage(PermissionRequestState state, String expectedMessage) {
        var message = new DatadisPermissionRequestStatusMessageResolver(state).getStatusMessage();
        assertEquals(expectedMessage, message);
    }
}