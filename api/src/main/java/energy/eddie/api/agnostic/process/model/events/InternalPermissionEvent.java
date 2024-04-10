package energy.eddie.api.agnostic.process.model.events;

/**
 * A marker interface to differentiate between events that should be propagated to the eligible party. If a permission
 * event implements this marker, it can be written to an internal database, but should not be emitted to the eligible
 * party.
 */
public interface InternalPermissionEvent {
}
