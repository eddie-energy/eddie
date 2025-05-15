package energy.eddie.aiida.models.monitoring.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

public record ServiceMetrics(
        @JsonProperty("timestamp") ZonedDateTime timestamp,
        @JsonProperty("serviceName") String serviceName,
        @JsonProperty("status") String status,
        @JsonProperty("cpuUsage") double cpuUsage,
        @JsonProperty("cpuUsageUnit") String cpuUsageUnit,
        @JsonProperty("memoryUsage") double memoryUsage,
        @JsonProperty("memoryUsageUnit") String memoryUsageUnit,
        @JsonProperty("networkOutgoing") double networkOutgoing,
        @JsonProperty("networkOutgoingUnit") String networkOutgoingUnit,
        @JsonProperty("networkIncoming") double networkIncoming,
        @JsonProperty("networkIncomingUnit") String networkIncomingUnit,
        @JsonProperty("uptime24h") double uptime24h,
        @JsonProperty("uptime24hUnit") String uptime24hUnit
) {}
