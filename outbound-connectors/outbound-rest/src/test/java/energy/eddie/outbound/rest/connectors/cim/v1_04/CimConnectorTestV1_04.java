package energy.eddie.outbound.rest.connectors.cim.v1_04;

import energy.eddie.cim.v1_04.rtd.RTDEnvelope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SuppressWarnings("java:S101")
class CimConnectorTestV1_04 {
    private final CimConnectorV1_04 connector = new CimConnectorV1_04();

    @Test
    void setRtd_producesRtds() {
        // Given
        var flux = Flux.just(new RTDEnvelope());

        // When
        connector.setNearRealTimeDataMarketDocumentStream(flux);

        // Then
        StepVerifier.create(connector.getNearRealTimeDataMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}