// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_12;

import energy.eddie.cim.v1_12.rtd.RTDEnvelope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class CimConnectorTest {
    private final CimConnector connector = new CimConnector();

    @Test
    void setRtd_producesRtds() {
        // Given
        var flux = Flux.just(new RTDEnvelope());

        // When
        connector.setNearRealTimeDataMarketDocumentStreamV1_12(flux);

        // Then
        StepVerifier.create(connector.getNearRealTimeDataMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}