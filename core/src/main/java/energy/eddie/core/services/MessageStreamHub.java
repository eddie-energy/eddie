// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.core.services;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class MessageStreamHub implements AutoCloseable {
    private final Map<Class<?>, Sinks.Many<Object>> sinksByType = new HashMap<>();
    private final Map<Class<?>, List<Consumer<Flux<?>>>> receiversByType = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized <T> void registerProvider(Class<T> messageType, Supplier<Flux<T>> provider) {
        var isPresent = sinksByType.containsKey(messageType);
        var sink = (Sinks.Many<T>) sinksByType.computeIfAbsent(
                messageType,
                key -> Sinks.many().multicast().onBackpressureBuffer()
        );

        provider.get().subscribe(sink::tryEmitNext);

        var receivers = receiversByType.get(messageType);
        if (receivers != null && !isPresent) {
            for (var receiver : receivers) {
                receiver.accept(sink.asFlux());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> void registerReceiver(Class<T> messageType, Consumer<Flux<?>> receiver) {
        receiversByType.computeIfAbsent(messageType, key -> new ArrayList<>()).add(receiver);

        var sink = (Sinks.Many<T>) sinksByType.get(messageType);
        if (sink != null) {
            receiver.accept(sink.asFlux());
        }
    }

    @Override
    public void close() {
        for (var sink : sinksByType.values()) {
            sink.tryEmitComplete();
        }
    }
}