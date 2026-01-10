package energy.eddie.regionconnector.de.eta.providers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EtaPlusMeteredData(
        String meteringPointId,
        LocalDate startDate,
        LocalDate endDate,
        List<MeterReading> readings,
        String rawJson
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MeterReading(
            String timestamp,
            Double value,
            String unit,
            @JsonProperty("status") String quality // Mapped from C# "status"
    ) {}
}