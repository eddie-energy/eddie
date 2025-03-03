package energy.eddie.regionconnector.cds.services.oauth;

import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientCredentials;
import energy.eddie.regionconnector.cds.client.customer.data.CustomerDataClientFactory;
import energy.eddie.regionconnector.cds.master.data.CdsServerBuilder;
import energy.eddie.regionconnector.cds.permission.events.SentToPaEvent;
import energy.eddie.regionconnector.cds.services.oauth.code.AuthorizationCodeResult;
import energy.eddie.regionconnector.shared.event.sourcing.Outbox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleAuthorizationServiceTest {
    @Mock
    private Outbox outbox;
    @Mock
    private OAuthService oAuthService;
    @Mock
    private CustomerDataClientFactory factory;
    @InjectMocks
    private SimpleAuthorizationService authorizationService;
    @Captor
    private ArgumentCaptor<SentToPaEvent> sentToPaEvent;

    @Test
    void testCreateOAuthRequest_returnsCorrectUriAndEmitsSentToPaEvent() {
        // Given
        var cdsServer = new CdsServerBuilder()
                .setId(1L)
                .build();
        var redirectUri = URI.create("http://localhost");
        when(factory.create(1L))
                .thenReturn(Mono.just(new CustomerDataClientCredentials("client-id")));
        when(oAuthService.createAuthorizationUri(eq(cdsServer), any(), any()))
                .thenReturn(new AuthorizationCodeResult(redirectUri, "state"));

        // When
        var res = authorizationService.createOAuthRequest(cdsServer, "pid");

        // Then
        StepVerifier.create(res)
                    .assertNext(uri -> assertEquals(redirectUri, uri))
                    .verifyComplete();
        verify(outbox).commit(sentToPaEvent.capture());
        var event = sentToPaEvent.getValue();
        assertFalse(event.isPushedAuthorizationRequest());
    }
}