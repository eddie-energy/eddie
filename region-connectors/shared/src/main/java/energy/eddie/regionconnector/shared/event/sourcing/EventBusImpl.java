package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class EventBusImpl implements EventBus, AutoCloseable {
    private static final Duration BUSY_LOOPING_DURATION = Duration.of(5, ChronoUnit.SECONDS);
    private final Sinks.Many<PermissionEvent> sink = Sinks.many().multicast().directAllOrNothing();
    private final Flux<PermissionEvent> flux = sink.asFlux().share();

    @Override
    public void emit(PermissionEvent event) {
        sink.emitNext(event, Sinks.EmitFailureHandler.busyLooping(BUSY_LOOPING_DURATION));
    }

    @Override
    public <T extends PermissionEvent> Flux<T> filteredFlux(Class<T> clazz) {
        return flux.ofType(clazz);
    }

    @Override
    public Flux<PermissionEvent> filteredFlux(PermissionProcessStatus status) {
        return flux.filter(permission -> permission.status().equals(status));
    }

    @Override
    public void close() {
        sink.emitComplete(Sinks.EmitFailureHandler.FAIL_FAST);
    }
}
