package energy.eddie.regionconnector.shared.event.sourcing;

import energy.eddie.api.agnostic.process.model.events.PermissionEvent;
import energy.eddie.api.v0.PermissionProcessStatus;
import reactor.core.publisher.Flux;

/**
 * Interface for a simple EventBus implementation.
 * Uses project reactor fluxes to notify subscribers of events.
 */
public interface EventBus {
    /**
     * Emit an event via the EventBus.
     * Every subscriber to this event will get the event.
     *
     * @param event event to be emitted
     */
    void emit(PermissionEvent event);

    /**
     * Get all events of a specific type.
     *
     * @param clazz the class of the events.
     * @param <T>   the type of the event
     * @return a flux, which streams the subscribed events.
     */
    <T extends PermissionEvent> Flux<T> filteredFlux(Class<T> clazz);

    /**
     * Get all events of a specific <code>PermissionProcessStatus</code>.
     *
     * @param status the <code>PermissionProcessStatus</code> of the event.
     * @return a flux, which streams the subscribed events.
     */
    Flux<PermissionEvent> filteredFlux(PermissionProcessStatus status);
}
