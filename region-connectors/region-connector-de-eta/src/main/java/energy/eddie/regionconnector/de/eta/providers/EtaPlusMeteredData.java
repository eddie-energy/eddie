package energy.eddie.regionconnector.de.eta.providers;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents metered data from the ETA Plus API.
 * This is a placeholder structure that should be replaced with the actual
 * ETA Plus API response format once the API specification is available.
 *
 * @param meteringPointId the metering point identifier
 * @param startDate       the start date of the metering period
 * @param endDate         the end date of the metering period
 * @param readings        the list of meter readings
 * @param rawJson         the original JSON response from ETA Plus for raw data emission
 */
public record EtaPlusMeteredData(
        String meteringPointId,
        LocalDate startDate,
        LocalDate endDate,
        List<MeterReading> readings,
        String rawJson
) {

    /**
     * Individual meter reading within a time series.
     *
     * @param timestamp the timestamp of the reading (ISO 8601)
     * @param value     the measured value
     * @param unit      the unit of measurement (e.g., kWh, Wh)
     * @param quality   the quality indicator of the reading
     */
    public record MeterReading(
            String timestamp,
            Double value,
            String unit,
            String quality
    ) {}
}

