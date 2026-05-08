package energy.eddie.regionconnector.de.eta.providers;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Metered data fetched from the ETA Plus historical-readings endpoint for a single metering point.
 *
 * @param meteringPointId the metering point identifier
 * @param startDate       inclusive lower bound of the request window
 * @param endDate         inclusive upper bound of the request window
 * @param readings        readings returned for the window
 */
public record EtaPlusMeteredData(
        String meteringPointId,
        LocalDate startDate,
        LocalDate endDate,
        List<MeterReading> readings
) {

    /**
     * One reading element from the ETA Plus historical-readings response.
     *
     * @param timestamp reading timestamp (UTC, per backend contract)
     * @param value     measured value
     * @param unit      wire unit string (e.g. {@code "kWh"}, {@code "m³"}); constant within a response
     * @param quality   wire status string; backend always emits {@code "VALIDATED"} on this endpoint
     * @param direction wire direction string; backend always emits {@code "Consumption"} or {@code "Generation"}
     */
    public record MeterReading(
            ZonedDateTime timestamp,
            Double value,
            String unit,
            String quality,
            String direction
    ) {}
}
