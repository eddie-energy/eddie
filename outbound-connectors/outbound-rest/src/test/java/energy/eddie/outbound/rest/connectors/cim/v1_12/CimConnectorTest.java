// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.outbound.rest.connectors.cim.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import energy.eddie.cim.v1_12.recmmoe.RECMMOEEnvelope;
import energy.eddie.cim.v1_12.rpmd.RequestPermissionEnvelope;
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

    @Test
    void setAcknowledgement_producesAcknowledgements() {
        // Given
        var ackFlux = Flux.just(new AcknowledgementEnvelope());

        // When
        connector.setAcknowledgementMarketDocumentStream(ackFlux);

        // Then
        StepVerifier.create(connector.getAcknowledgementMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setPublish_producesMinMaxEnvelope() {
        // Given
        var minMaxEnvelope = new RECMMOEEnvelope();
        // When
        connector.publish(minMaxEnvelope);

        // Then
        StepVerifier.create(connector.getMinMaxEnvelopes())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setMinMaxEnvelopeStream_producesMinMaxEnvelope() {
        // Given
        var minMaxEnvelopeStream = Flux.just(new RECMMOEEnvelope());

        // When
        connector.setMinMaxEnvelopeStream(minMaxEnvelopeStream);

        // Then
        StepVerifier.create(connector.getMinMaxEnvelopes())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setEnergySharingReferenceDataMarketDocument_producesEnergySharingReferenceDataMarketDocument() {
        // Given
        var esrFlux = Flux.just(new ESRDMDEnvelope());

        // When
        connector.setEnergySharingReferenceDataMarketDocumentStream(esrFlux);

        // Then
        StepVerifier.create(connector.getEnergySharingReferenceDataMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void setRequestPermissionMarketDocument_producesRequestPermissionMarketDocument() {
        // Given
        var rpmdFlux = Flux.just(new RequestPermissionEnvelope());

        // When
        connector.setRequestPermissionMarketDocumentStream(rpmdFlux);

        // Then
        StepVerifier.create(connector.getRequestPermissionMarketDocumentStream())
                    .then(connector::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }
}