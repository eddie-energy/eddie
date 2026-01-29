// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services.v0_82;

import energy.eddie.api.v0_82.AccountingPointEnvelopeProvider;
import energy.eddie.cim.v0_82.ap.AccountingPointEnvelope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;

class AccountingPointEnvelopeServiceTest {
    @BeforeAll
    static void beforeAll() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(2));
    }

    @Test
    void givenMultipleStreams_combinesAndEmitsAllValuesFromAllStreams() throws Exception {
        // Given
        var service = new AccountingPointEnvelopeService();
        Sinks.Many<AccountingPointEnvelope> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<AccountingPointEnvelope> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        AccountingPointEnvelopeProvider provider1 = createProvider(sink1);
        AccountingPointEnvelopeProvider provider2 = createProvider(sink2);

        var one = new AccountingPointEnvelope();
        var two = new AccountingPointEnvelope();
        var three = new AccountingPointEnvelope();

        // When
        var flux = service.getAccountingPointEnvelopeStream();
        StepVerifier.create(flux)
                    .then(() -> {
                        service.registerProvider(provider1);
                        service.registerProvider(provider2);
                        sink2.tryEmitNext(two);
                        sink1.tryEmitNext(one);
                        sink2.tryEmitNext(three);
                    })
                    // Then
                    .expectNextCount(3)
                    .thenCancel()
                    .verify();

        provider1.close();
        provider2.close();
    }

    private static AccountingPointEnvelopeProvider createProvider(Sinks.Many<AccountingPointEnvelope> sink) {
        return new AccountingPointEnvelopeProvider() {
            @Override
            public Flux<AccountingPointEnvelope> getAccountingPointEnvelopeFlux() {
                return sink.asFlux();
            }

            @Override
            public void close() {
                sink.tryEmitComplete();
            }
        };
    }
}
