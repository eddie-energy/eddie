package api;

/**
 * Record holding the description of an error that occurred when an API of EDDIE was called.
 * Can be extended by more fields.
 * It is intended that an API controller advice returns a List of these errors.
 *
 * @param message Message describing the occurred error.
 */
public record EddieApiError(String message) {
}
