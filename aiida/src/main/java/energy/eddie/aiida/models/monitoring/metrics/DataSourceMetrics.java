package energy.eddie.aiida.models.monitoring.metrics;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DataSourceMetrics(
        @JsonProperty("timestamp") String timestamp,
        @JsonProperty("dataSourceType") String dataSourceType,
        @JsonProperty("name") String name,
        @JsonProperty("messageCountLast24Hours") long messageCountLast24Hours,
        @JsonProperty("messageCountLastHour") long messageCountLastHour,
        @JsonProperty("messageCountLastMinute") long messageCountLastMinute,
        @JsonProperty("lastMessageTimestamp") String lastMessageTimestamp) { }
