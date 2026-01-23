// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.event.sourcing.handlers;

/**
 * The interface for event handlers.
 * Can be combined with the event bus, to handle events emitted by it.
 * Should be used by subscribing to the event bus with the accept method in the constructor.
 * <pre>
 *     public class EventHandlerImpl implements EventHandler&lt;PermissionEvent&gt; {
 *         public EventHandlerImpl(EventBus eventBus) {
 *             eventBus.flux(PermissionEvent.class)
 *               .subscribe(this::accept);
 *         }
 *
 *         public void accept(PermissionEvent permissionEvent) {
 *             // Do something with the event
 *         }
 *     }
 * </pre>
 *
 * @param <T> The type of event that should be handled.
 */
public interface EventHandler<T> {

    /**
     * Subscription method for the event bus
     *
     * @param permissionEvent event that indicates that a permission request has been changed
     */
    void accept(T permissionEvent);
}
