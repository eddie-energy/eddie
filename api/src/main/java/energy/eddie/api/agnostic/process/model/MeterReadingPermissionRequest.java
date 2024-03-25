package energy.eddie.api.agnostic.process.model;

import energy.eddie.api.agnostic.process.model.annotations.InvokeExtensions;

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

    /**
     * Update the latest meter reading end date that was pulled or received for this permission request.
     *
     * @param date The latest meter reading end date that was pulled or received for this permission request.
     */
    @InvokeExtensions
    void updateLatestMeterReadingEndDate(LocalDate date);
}
