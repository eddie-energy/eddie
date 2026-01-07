package energy.eddie.outbound.rest.web.cim.v0_91_08;

import energy.eddie.api.agnostic.retransmission.result.*;
import energy.eddie.cim.v0_91_08.ESMPDateTimeInterval;
import energy.eddie.cim.v0_91_08.RTREnvelope;
import energy.eddie.outbound.rest.connectors.RestRetransmissionConnector;
import energy.eddie.outbound.rest.web.WebTestConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(value = RedistributionTransactionRequestController.class, excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class)
@Import({WebTestConfig.class})
class RedistributionTransactionRequestControllerTest {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private RestRetransmissionConnector connector;

    @ParameterizedTest
    @MethodSource("getRetransmissionResults")
    void redistributionTransactionRd_returnsResult(RetransmissionResult result, HttpStatus httpStatus) {
        // Given
        var doc = new RTREnvelope()
                .withMarketDocumentMRID("pid")
                .withMessageDocumentHeaderMetaInformationPermissionId("pid")
                .withMarketDocumentPeriodTimeInterval(
                        new ESMPDateTimeInterval()
                                .withStart("2025-01-01T00:00Z")
                                .withEnd("2025-01-09T00:00Z")
                );
        when(connector.publish(any())).thenReturn(Mono.just(result));

        // When
        webTestClient.post()
                     .uri("/cim_0_91_08/redistribution-transaction-rd")
                     .bodyValue(doc)
                     .exchange()
                     // Then
                     .expectStatus()
                     .isEqualTo(httpStatus);
    }

    private static Stream<Arguments> getRetransmissionResults() {
        var now = ZonedDateTime.now(ZoneOffset.UTC);
        return Stream.of(
                Arguments.of(new DataNotAvailable("pid", now), HttpStatus.NO_CONTENT),
                Arguments.of(new Failure("pid", now, "reason"), HttpStatus.INTERNAL_SERVER_ERROR),
                Arguments.of(new NoActivePermission("pid", now), HttpStatus.GONE),
                Arguments.of(new NoPermissionForTimeFrame("pid", now), HttpStatus.CONFLICT),
                Arguments.of(new NotSupported("pid", now, "reason"), HttpStatus.CONFLICT),
                Arguments.of(new PermissionRequestNotFound("pid", now), HttpStatus.NOT_FOUND),
                Arguments.of(new RetransmissionServiceNotFound("pid", "rc-id", now), HttpStatus.NOT_FOUND),
                Arguments.of(new Success("pid", now), HttpStatus.OK)
        );
    }
}