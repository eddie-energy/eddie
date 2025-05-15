package energy.eddie.aiida.models.monitoring.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record HostMetrics(
        @JsonProperty("timestamp") ZonedDateTime timestamp,
        @JsonProperty("hostname") String hostname,
        @JsonProperty("cpuUsage") double cpuUsage,
        @JsonProperty("cpuUsageUnit") String cpuUsageUnit,
        @JsonProperty("memoryUsage") double memoryUsage,
        @JsonProperty("memoryUsageUnit") String memoryUsageUnit,
        @JsonProperty("uptime") double uptime,
        @JsonProperty("uptimeUnit") String uptimeUnit,
        @JsonProperty("networkOutgoing") double networkOutgoing,
        @JsonProperty("networkOutgoingUnit") String networkOutgoingUnit,
        @JsonProperty("networkIncoming") double networkIncoming,
        @JsonProperty("networkIncomingUnit") String networkIncomingUnit,
        @JsonProperty("diskUsage") double diskUsage,
        @JsonProperty("diskUsageUnit") String diskUsageUnit
) {}
