// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.api.v1_12.EnergySharingReferenceDataMarketDocumentProvider;
import energy.eddie.cim.v1_12.esr.ESRDMDEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class EnergySharingReferenceDataMarketDocumentServiceTest {

    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() {
        // Given
        var service = new EnergySharingReferenceDataMarketDocumentService();
        Sinks.Many<ESRDMDEnvelope> publisher1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<ESRDMDEnvelope> publisher2 = Sinks.many().unicast().onBackpressureBuffer();

        EnergySharingReferenceDataMarketDocumentProvider provider1 = publisher1::asFlux;
        EnergySharingReferenceDataMarketDocumentProvider provider2 = publisher2::asFlux;

        var one = new ESRDMDEnvelope();
        var two = new ESRDMDEnvelope();
        var three = new ESRDMDEnvelope();

        // When
        var flux = service.getEnergySharingReferenceDataMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> {
                        service.registerProvider(provider1);
                        service.registerProvider(provider2);
                        publisher2.tryEmitNext(two);
                        publisher1.tryEmitNext(one);
                        publisher2.tryEmitNext(three);
                    })
                    // Then
                    .expectNextCount(3)
                    .thenCancel()
                    .verify();
    }
}