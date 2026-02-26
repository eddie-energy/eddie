// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v1_12;

import energy.eddie.cim.v1_12.ack.AcknowledgementEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class AcknowledgementMarketDocumentServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() {
        // Given
        var service = new AcknowledgementMarketDocumentService();
        TestPublisher<AcknowledgementEnvelope> publisher1 = TestPublisher.create();
        TestPublisher<AcknowledgementEnvelope> publisher2 = TestPublisher.create();

        service.registerProvider(publisher1::flux);
        service.registerProvider(publisher2::flux);

        var one = new AcknowledgementEnvelope();
        var two = new AcknowledgementEnvelope();
        var three = new AcknowledgementEnvelope();

        // When
        var flux = service.getAcknowledgementMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> {
                        publisher2.next(two);
                        publisher1.next(one);
                        publisher2.next(three);
                    })
                    // Then
                    .expectNextCount(3)
                    .thenCancel()
                    .verify();
    }

    @Test
    void givenConverter_appliesItToStream() {
        // Given
        var service = new AcknowledgementMarketDocumentService();
        TestPublisher<AcknowledgementEnvelope> publisher = TestPublisher.create();

        service.registerProvider(publisher::flux);

        var one = new AcknowledgementEnvelope();
        // When
        var flux = service.getAcknowledgementMarketDocumentStream();
        StepVerifier.create(flux)
                    .then(() -> publisher.next(one))
                    // Then
                    .expectNextCount(1)
                    .thenCancel()
                    .verify();
    }
}