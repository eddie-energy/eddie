package energy.eddie.regionconnector.shared.timeout;


/**
 * Configuration to set the timeouts of permission requests.
 *
 * @param duration the amount of time in hours after which a permission request is considered stale
 */
public record TimeoutConfiguration(int duration) {
}
