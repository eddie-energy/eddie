// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.shared.timeout;


/**
 * Configuration to set the timeouts of permission requests.
 *
 * @param duration the amount of time in hours after which a permission request is considered stale
 */
public record TimeoutConfiguration(int duration) {
    public TimeoutConfiguration {
        if (duration <= 0) {
            throw new InvalidTimeoutConfigurationException("duration must be greater than 0");
        }
    }
}
