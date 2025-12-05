package energy.eddie.regionconnector.de.eta.client;

import energy.eddie.regionconnector.de.eta.oauth.DeEtaOAuthTokenService;
import energy.eddie.regionconnector.de.eta.permission.requests.DateRange;
import energy.eddie.regionconnector.de.eta.permission.requests.DeEtaPermissionRequest;
import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DeEtaPaApiClientImplTest {

    private DeEtaPermissionRequest sampleRequest() {
        var created = ZonedDateTime.now(ZoneOffset.UTC);
        var dr = new DateRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1));
        return new DeEtaPermissionRequest("perm-1", "conn-1", "consumption_interval", PermissionProcessStatus.VALIDATED, created, dr, "PT15M");
    }

    private WebClient clientWith(ExchangeFunction fn) {
        return WebClient.builder().exchangeFunction(fn).build();
    }

    private DeEtaPaApiClientImpl build(WebClient wc) {
        var tokenSvc = Mockito.mock(DeEtaOAuthTokenService.class);
        when(tokenSvc.getValidAccessToken("conn-1")).thenReturn("tkn");
        // Provide all required properties; use 0 retries and tiny backoff for fast tests
        var props = new DeEtaPaApiProperties("https://pa.example/permissions", 100, 2000, 0, 10);
        return new DeEtaPaApiClientImpl(wc, tokenSvc, props);
    }

    @Test
    void sendPermissionRequest_success201_returnsSuccessTrue() {
        ExchangeFunction fn = (ClientRequest req) -> {
            assertEquals(URI.create("https://pa.example/permissions"), req.url());
            assertEquals("Bearer tkn", req.headers().getFirst("Authorization"));
            return Mono.just(ClientResponse.create(HttpStatus.CREATED).header("Content-Type", MediaType.APPLICATION_JSON_VALUE).build());
        };
        var api = build(clientWith(fn));
        var resp = api.sendPermissionRequest(sampleRequest()).block();
        assertNotNull(resp);
        assertTrue(resp.success());
    }

    @Test
    void sendPermissionRequest_clientError400_returnsSuccessFalse() {
        ExchangeFunction fn = (ClientRequest req) -> Mono.just(ClientResponse.create(HttpStatus.BAD_REQUEST).build());
        var api = build(clientWith(fn));
        var resp = api.sendPermissionRequest(sampleRequest()).block();
        assertNotNull(resp);
        assertFalse(resp.success());
    }

    @Test
    void sendPermissionRequest_serverError500_throwsTransientPaException() {
        ExchangeFunction fn = (ClientRequest req) -> Mono.just(ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());
        var api = build(clientWith(fn));
        // Refactor to ensure the lambda has only one potentially throwing invocation
        var mono = api.sendPermissionRequest(sampleRequest());
        var ex = assertThrows(TransientPaException.class, mono::block);
        assertTrue(ex.getMessage().contains("PA responded"));
    }
}
