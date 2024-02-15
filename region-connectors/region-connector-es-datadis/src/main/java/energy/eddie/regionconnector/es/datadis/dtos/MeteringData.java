package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import energy.eddie.regionconnector.es.datadis.deserializer.MeteringDataDeserializer;

import java.time.ZonedDateTime;

/**
 * This class represents the metering data returned by the Datadis API.
 * It contains both consumption and surplus energy (production) data.
 */
@JsonDeserialize(using = MeteringDataDeserializer.class)
public record MeteringData(
        String cups,
        ZonedDateTime dateTime,
        Double consumptionKWh,
        String obtainMethod,
        Double surplusEnergyKWh) {
}