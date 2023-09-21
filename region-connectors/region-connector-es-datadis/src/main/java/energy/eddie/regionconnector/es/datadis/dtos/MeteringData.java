package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * This class represents the metering data returned by the Datadis API.
 * It contains both consumption and surplus energy (production) data.
 */
public record MeteringData(
        String cups,
        @JsonFormat(pattern = "yyyy/MM/dd") LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime time,
        Double consumptionKWh,
        String obtainMethod,
        Double surplusEnergyKWh) {
}
