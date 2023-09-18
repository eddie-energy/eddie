package energy.eddie.regionconnector.es.datadis.dtos;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import energy.eddie.regionconnector.es.datadis.serializer.LocalDateToEpochSerializer;

import java.time.LocalDate;

public record Cups(
        String cups,
        @JsonSerialize(using = LocalDateToEpochSerializer.class)
        LocalDate startDate,
        @JsonSerialize(using = LocalDateToEpochSerializer.class)
        LocalDate endDate) {
}
