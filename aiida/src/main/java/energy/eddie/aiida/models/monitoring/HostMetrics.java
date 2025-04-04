package energy.eddie.aiida.models.monitoring;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HostMetrics (
    @JsonProperty("hostname") String name,
    @JsonProperty("cpuUsage") double cpuUsage,
    @JsonProperty("memoryUsage") double memoryUsage
) {}