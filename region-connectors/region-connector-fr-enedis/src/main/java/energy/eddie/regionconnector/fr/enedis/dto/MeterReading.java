package energy.eddie.regionconnector.fr.enedis.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

public record MeterReading(
        @JsonProperty("usage_point_id")
        String usagePointId,
        @JsonProperty("start")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate start,
        @JsonProperty("end")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate end,
        @JsonProperty("quality")
        String quality,
        @JsonProperty("reading_type")
        ReadingType readingType,
        @JsonProperty("interval_reading")
        List<IntervalReading> intervalReadings) {
}