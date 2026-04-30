// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.util.function.Consumer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageStreamHubTest {
    @InjectMocks
    private MessageStreamHub messageStreamHub;
    @Captor
    private ArgumentCaptor<Flux<Integer>> fluxCaptor;
    @Mock
    private Consumer<Flux<?>> consumer;

    @Test
    void testRegisterReceiverBeforeProvider() {
        // Given
        Sinks.Many<Object> receiverSink = Sinks.many().unicast().onBackpressureBuffer();
        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(receiverSink::tryEmitNext)
        );

        Sinks.Many<Integer> sink = Sinks.many().unicast().onBackpressureBuffer();
        Flux<Integer> provider = sink.asFlux();

        // When
        messageStreamHub.registerProvider(Integer.class, () -> provider);
        sink.tryEmitNext(1);
        sink.tryEmitNext(2);

        // Then
        StepVerifier.create(receiverSink.asFlux().take(2))
                    .expectNext(1)
                    .expectNext(2)
                    .verifyComplete();
    }

    @Test
    void testRegisterReceiverAfterProvider() {
        // Given
        Sinks.Many<Integer> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<Integer> provider = sink.asFlux();
        messageStreamHub.registerProvider(Integer.class, () -> provider);
        Sinks.Many<Object> receiverSink = Sinks.many().unicast().onBackpressureBuffer();

        // When
        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(receiverSink::tryEmitNext)
        );

        sink.tryEmitNext(1);
        sink.tryEmitNext(2);

        // Then
        StepVerifier.create(receiverSink.asFlux().take(2))
                    .expectNext(1)
                    .expectNext(2)
                    .verifyComplete();
    }

    @Test
    void testMultipleReceiversForSameMessageType() {
        // Given
        Sinks.Many<Integer> sink = Sinks.many().multicast().onBackpressureBuffer();
        Flux<Integer> provider = sink.asFlux();

        messageStreamHub.registerProvider(Integer.class, () -> provider);

        Sinks.Many<Object> receiver1Sink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<Object> receiver2Sink = Sinks.many().unicast().onBackpressureBuffer();
        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(receiver1Sink::tryEmitNext)
        );

        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(receiver2Sink::tryEmitNext)
        );

        // When
        sink.tryEmitNext(1);
        sink.tryEmitNext(2);

        // Then
        StepVerifier.create(receiver1Sink.asFlux().take(2))
                    .expectNext(1)
                    .expectNext(2)
                    .verifyComplete();

        StepVerifier.create(receiver2Sink.asFlux().take(2))
                    .expectNext(1)
                    .expectNext(2)
                    .verifyComplete();
    }

    @Test
    void testMultipleProvidersForSameMessageType() {
        // Given
        Sinks.Many<Integer> sink1 = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<Integer> sink2 = Sinks.many().unicast().onBackpressureBuffer();

        Sinks.Many<Object> receiverSink = Sinks.many().unicast().onBackpressureBuffer();

        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(receiverSink::tryEmitNext)
        );
        messageStreamHub.registerProvider(Integer.class, sink1::asFlux);
        messageStreamHub.registerProvider(Integer.class, sink2::asFlux);

        // When
        sink1.tryEmitNext(1);
        sink1.tryEmitNext(2);
        sink2.tryEmitNext(3);
        sink2.tryEmitNext(4);

        // Then
        StepVerifier.create(receiverSink.asFlux().take(4))
                    .expectNextCount(4)
                    .verifyComplete();
    }

    @Test
    void testDifferentMessageTypes() {
        // Given
        Sinks.Many<String> stringSink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<Integer> integerSink = Sinks.many().unicast().onBackpressureBuffer();

        Sinks.Many<Object> stringReceiverSink = Sinks.many().unicast().onBackpressureBuffer();
        Sinks.Many<Object> integerReceiverSink = Sinks.many().unicast().onBackpressureBuffer();
        messageStreamHub.registerReceiver(String.class, flux ->
                flux.subscribe(stringReceiverSink::tryEmitNext)
        );

        messageStreamHub.registerReceiver(Integer.class, flux ->
                flux.subscribe(integerReceiverSink::tryEmitNext)
        );

        messageStreamHub.registerProvider(String.class, stringSink::asFlux);
        messageStreamHub.registerProvider(Integer.class, integerSink::asFlux);

        // When
        stringSink.tryEmitNext("String 1");
        stringSink.tryEmitNext("String 2");
        integerSink.tryEmitNext(1);
        integerSink.tryEmitNext(2);

        // Then
        StepVerifier.create(stringReceiverSink.asFlux().take(2))
                    .expectNext("String 1")
                    .expectNext("String 2")
                    .verifyComplete();

        StepVerifier.create(integerReceiverSink.asFlux().take(2))
                    .expectNext(1)
                    .expectNext(2)
                    .verifyComplete();
    }

    @Test
    void testMultipleNewProvidersWillNotLeadToDoubleSubscriptions() {
        // Given
        TestPublisher<Integer> testPublisher1 = TestPublisher.create();
        TestPublisher<Integer> testPublisher2 = TestPublisher.create();

        // When
        messageStreamHub.registerProvider(Integer.class, testPublisher1::flux);
        messageStreamHub.registerReceiver(Integer.class, consumer);
        messageStreamHub.registerProvider(Integer.class, testPublisher2::flux);

        // Then
        verify(consumer).accept(fluxCaptor.capture());
        StepVerifier.create(fluxCaptor.getValue())
                    .then(() -> {
                        testPublisher1.next(1);
                        testPublisher2.next(3);
                        testPublisher1.next(2);
                    })
                    .expectNext(1)
                    .expectNext(3)
                    .expectNext(2)
                    .then(messageStreamHub::close)
                    .verifyComplete();
    }
}