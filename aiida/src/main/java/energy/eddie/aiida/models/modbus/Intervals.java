package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Intervals(
        @JsonProperty("read") IntervalConfig read
) {}


