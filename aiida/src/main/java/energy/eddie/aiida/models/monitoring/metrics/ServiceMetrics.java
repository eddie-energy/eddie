package energy.eddie.aiida.models.monitoring.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ServiceMetrics (
        @JsonProperty("podName") String name,
        @JsonProperty("status") String status,
        @JsonProperty("cpuUsage") double cpuUsage,
        @JsonProperty("memoryUsage") double memoryUsage,
        @JsonProperty("uptime24h") double uptime24h
) {}