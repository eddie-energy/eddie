package energy.eddie.aiida.models.modbus;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IntervalConfig(
        @JsonProperty("default") long defaultInterval,
        @JsonProperty("min") long minInterval
) {
}