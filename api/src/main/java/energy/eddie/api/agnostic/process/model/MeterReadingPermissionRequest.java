// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.api.agnostic.process.model;

import java.time.LocalDate;
import java.util.Optional;

/**
 * A permission request that stores date information about the latest meter reading that was pulled or received.
 */
public interface MeterReadingPermissionRequest extends PermissionRequest {
    /**
     * The latest meter reading end date that was pulled or received for this permission request.
     */
    Optional<LocalDate> latestMeterReadingEndDate();
}
