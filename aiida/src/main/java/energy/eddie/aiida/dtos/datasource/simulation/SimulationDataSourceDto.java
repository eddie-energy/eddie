// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.dtos.datasource.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.IntervalBasedDataSourceDto;
import io.swagger.v3.oas.annotations.media.Schema;

@SuppressWarnings({"NullAway.Init"})
public class SimulationDataSourceDto extends IntervalBasedDataSourceDto {
    @Schema(description = "Minimum instantaneous active power for consumption (import) in watt.")
    @JsonProperty
    protected Long minConsumptionWatt;

    @Schema(description = "Maximum instantaneous active power for consumption (import) in watt.")
    @JsonProperty
    protected Long maxConsumptionWatt;

    @Schema(description = "Minimum instantaneous active power for production (export) in watt.")
    @JsonProperty
    protected Long minProductionWatt;

    @Schema(description = "Maximum instantaneous active power for production (export) in watt.")
    @JsonProperty
    protected Long maxProductionWatt;

    public Long minConsumptionWatt() {
        return minConsumptionWatt;
    }

    public Long maxConsumptionWatt() {
        return maxConsumptionWatt;
    }

    public Long minProductionWatt() {
        return minProductionWatt;
    }

    public Long maxProductionWatt() {
        return maxProductionWatt;
    }
}
