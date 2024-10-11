package energy.eddie.regionconnector.be.fluvius.clients;

import energy.eddie.regionconnector.be.fluvius.client.model.CreateMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.FluviusSessionCreateResultResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.client.model.GetMandateResponseModelApiDataResponse;
import energy.eddie.regionconnector.be.fluvius.permission.request.Flow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SandboxFluviusApiClientTest {
    @Mock
    private FluviusApi api;
    @InjectMocks
    private SandboxFluviusApiClient sandboxFluviusApiClient;

    @Test
    void testShortUrlIdentifier_callsMockMandate() {
        // Given
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        when(api.mockMandate("pid", now, now))
                .thenReturn(Mono.just(new CreateMandateResponseModelApiDataResponse()));
        when(api.shortUrlIdentifier("pid", Flow.B2B, now, now))
                .thenReturn(Mono.just(new FluviusSessionCreateResultResponseModelApiDataResponse()));

        // When
        var res = sandboxFluviusApiClient.shortUrlIdentifier("pid", Flow.B2B, now, now);

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testMandateFor_callsDecoratee() {
        // Given
        when(api.mandateFor("pid"))
                .thenReturn(Mono.just(new GetMandateResponseModelApiDataResponse()));

        // When
        var res = sandboxFluviusApiClient.mandateFor("pid");

        // Then
        StepVerifier.create(res)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}