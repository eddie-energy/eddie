package energy.eddie.regionconnector.de.eta.dtos;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.api.agnostic.data.needs.EnergyType;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Represents validated historical metering data from the ETA Plus API.
 * This is a placeholder structure - the actual fields should match the ETA Plus API response format.
 *
 * @param fetchTime When the data was fetched from the API
 * @param meteringPointId The metering point identifier
 * @param energyType The type of energy (electricity or gas)
 * @param readings The list of meter readings
 */
public record EtaMeteringDataPayload(
        @Nullable ZonedDateTime fetchTime,
        String meteringPointId,
        EnergyType energyType,
        List<MeterReading> readings
) {
    /**
     * Represents a single meter reading.
     *
     * @param timestamp The timestamp of the reading
     * @param value The measured value
     * @param unit The unit of measurement (e.g., "kWh", "m3")
     * @param granularity The granularity of the measurement
     * @param quality The quality indicator for the measurement
     */
    public record MeterReading(
            ZonedDateTime timestamp,
            double value,
            String unit,
            Granularity granularity,
            QualityIndicator quality
    ) {
    }

    /**
     * Quality indicator for meter readings.
     */
    public enum QualityIndicator {
        MEASURED,
        ESTIMATED,
        SUBSTITUTED
    }
}

