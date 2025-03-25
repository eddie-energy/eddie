package energy.eddie.regionconnector.cds.client.customer.data;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.exceptions.NoTokenException;
import energy.eddie.regionconnector.cds.permission.requests.CdsPermissionRequestBuilder;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerDataClientErrorHandlerTest {
    @Mock
    private Outbox outbox;
    @InjectMocks
    private CustomerDataClientErrorHandler handler;

    public static Stream<Arguments> testPredicate_whenExceptionIndicatesRevocation_returnsTrue() {
        return Stream.of(
                Arguments.of(WebClientResponseException.create(HttpStatus.UNAUTHORIZED,
                                                               "text",
                                                               null,
                                                               null,
                                                               null,
                                                               null)),
                Arguments.of(new NoTokenException())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testPredicate_whenExceptionIndicatesRevocation_returnsTrue(Exception e) {
        // Given
        // When
        var res = handler.test(e);
        // Then
        assertTrue(res);
    }

    @Test
    void testPredicate_whenExceptionDoesNotIndicateRevocation_returnsFalse() {
        // Given
        // When
        var res = handler.test(new IOException());
        // Then
        assertFalse(res);
    }

    @Test
    void testThenRevoke_emitsRevokedEvent() {
        // Given
        var e = new NoTokenException();
        var pr = new CdsPermissionRequestBuilder()
                .setPermissionId("pid")
                .build();
        var revocationHandler = handler.thenRevoke(pr);

        // When
        revocationHandler.accept(e);

        // Then
        verify(outbox).commit(assertArg(event -> assertAll(
                () -> assertEquals("pid", event.permissionId()),
                () -> assertEquals(PermissionProcessStatus.REVOKED, event.status())
        )));
    }
}