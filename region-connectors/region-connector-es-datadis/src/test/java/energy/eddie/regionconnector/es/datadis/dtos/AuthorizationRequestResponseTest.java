package energy.eddie.regionconnector.es.datadis.dtos;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationRequestResponseTest {

    private static Stream<Arguments> expectedClasses() {
        return Stream.of(
                Arguments.of("ok", AuthorizationRequestResponse.Ok.class),
                Arguments.of("nonif", AuthorizationRequestResponse.NoNif.class),
                Arguments.of("nopermisos", AuthorizationRequestResponse.NoPermission.class),
                Arguments.of("xxx", AuthorizationRequestResponse.Unknown.class)

        );
    }

    @ParameterizedTest
    @MethodSource("expectedClasses")
    void fromResponse(String response, Class<?> expectedClass) {
        var authorizationRequestResponse = AuthorizationRequestResponse.fromResponse(response);
        assertTrue(expectedClass.isInstance(authorizationRequestResponse));
    }

    @ParameterizedTest
    @MethodSource("expectedClasses")
    void originalResponse(String response, Class<?> expectedClass) {
        var authorizationRequestResponse = AuthorizationRequestResponse.fromResponse(response);
        assertEquals(response, authorizationRequestResponse.originalResponse());
    }
}