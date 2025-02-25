package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.services.oauth.par.ErrorParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.ParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.SuccessfulParResponse;
import energy.eddie.regionconnector.cds.services.oauth.par.UnableToSendPar;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushedAuthorizationServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private OAuthService oAuthService;
    @InjectMocks
    private PushedAuthorizationService authorizationService;
    @Captor
    private ArgumentCaptor<SentToPaEvent> sentToPaEvent;

    public static Stream<Arguments> testCreateOAuthRequest_withInvalidParResponse_returnsNull() {
        return Stream.of(
                Arguments.of(new ErrorParResponse("error")),
                Arguments.of(new UnableToSendPar())
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCreateOAuthRequest_withInvalidParResponse_returnsNull(ParResponse parResponse) {
        // Given
        var cdsServer = new CdsServerBuilder().build();
        when(oAuthService.pushAuthorization(eq(cdsServer), any())) .thenReturn(parResponse);

        // When
        var res = authorizationService.createOAuthRequest(cdsServer, "pid");

        // Then
        assertNull(res);
        verify(outbox).commit(assertArg(event -> assertEquals(PermissionProcessStatus.UNABLE_TO_SEND, event.status())));
    }

    @Test
    void testCreateOAuthRequest_withValidParResponse_returnsUrl() {
        // Given
        var cdsServer = new CdsServerBuilder().build();
        var redirectUri = URI.create("http://localhost");
        var parResponse = new SuccessfulParResponse(redirectUri, ZonedDateTime.now(ZoneOffset.UTC), "state");
        when(oAuthService.pushAuthorization(eq(cdsServer), any())) .thenReturn(parResponse);

        // When
        var res = authorizationService.createOAuthRequest(cdsServer, "pid");

        // Then
        assertEquals(redirectUri, res);
        verify(outbox).commit(sentToPaEvent.capture());
        var event = sentToPaEvent.getValue();
        assertTrue(event.isPushedAuthorizationRequest());
    }
}