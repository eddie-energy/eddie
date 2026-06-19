// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.shelly.transformer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShellyPlugGen3Json(
        @JsonProperty("src") String source,
        @JsonProperty("dst") String destination,
        @JsonProperty("method") String method,
        @JsonProperty("params") Params params
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Params(
            @JsonProperty("ts") Double timestamp,
            @JsonProperty("switch:0") SwitchComponent switch0
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SwitchComponent(
            @JsonProperty("id") Integer id,
            @JsonProperty("output") Boolean output,
            @JsonProperty("apower") Double apower,
            @JsonProperty("voltage") Double voltage,
            @JsonProperty("current") Double current,
            @JsonProperty("freq") Double freq,
            @JsonProperty("aenergy") Aenergy aenergy
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Aenergy(
            @JsonProperty("total") Double total,
            @JsonProperty("by_minute") java.util.List<Double> byMinute
    ) {}
}