// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.interval.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.errors.datasource.InvalidSimulationRangeException;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.interval.IntervalDataSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

import java.util.Objects;
import java.util.UUID;

@Entity
@SecondaryTable(name = SimulationDataSource.TABLE_NAME)
@DiscriminatorValue(DataSourceType.Identifiers.SIMULATION)
public class SimulationDataSource extends IntervalDataSource {
    protected static final String TABLE_NAME = "data_source_simulation";

    public static final long DEFAULT_MIN_CONSUMPTION_WATT = 200;
    public static final long DEFAULT_MAX_CONSUMPTION_WATT = 1800;
    public static final long DEFAULT_MIN_PRODUCTION_WATT = 0;
    public static final long DEFAULT_MAX_PRODUCTION_WATT = 1200;

    @Column(name = "min_consumption_watt", table = TABLE_NAME, nullable = false)
    @Schema(description = "Minimum instantaneous active power for consumption (import) in watt.")
    @JsonProperty
    private long minConsumptionWatt = DEFAULT_MIN_CONSUMPTION_WATT;

    @Column(name = "max_consumption_watt", table = TABLE_NAME, nullable = false)
    @Schema(description = "Maximum instantaneous active power for consumption (import) in watt.")
    @JsonProperty
    private long maxConsumptionWatt = DEFAULT_MAX_CONSUMPTION_WATT;

    @Column(name = "min_production_watt", table = TABLE_NAME, nullable = false)
    @Schema(description = "Minimum instantaneous active power for production (export) in watt.")
    @JsonProperty
    private long minProductionWatt = DEFAULT_MIN_PRODUCTION_WATT;

    @Column(name = "max_production_watt", table = TABLE_NAME, nullable = false)
    @Schema(description = "Maximum instantaneous active power for production (export) in watt.")
    @JsonProperty
    private long maxProductionWatt = DEFAULT_MAX_PRODUCTION_WATT;

    @SuppressWarnings("NullAway")
    protected SimulationDataSource() {}

    public SimulationDataSource(SimulationDataSourceDto dto, UUID userId) {
        super(dto, userId);
        applyDto(dto);
    }

    @Override
    public void update(DataSourceDto dto) {
        super.update(dto);
        if (dto instanceof SimulationDataSourceDto simulationDto) {
            applyDto(simulationDto);
        }
    }

    public long minConsumptionWatt() {
        return minConsumptionWatt;
    }

    public long maxConsumptionWatt() {
        return maxConsumptionWatt;
    }

    public long minProductionWatt() {
        return minProductionWatt;
    }

    public long maxProductionWatt() {
        return maxProductionWatt;
    }

    private void applyDto(SimulationDataSourceDto dto) {
        var newMinConsumptionWatt = Objects.requireNonNullElse(dto.minConsumptionWatt(), DEFAULT_MIN_CONSUMPTION_WATT);
        var newMaxConsumptionWatt = Objects.requireNonNullElse(dto.maxConsumptionWatt(), DEFAULT_MAX_CONSUMPTION_WATT);
        var newMinProductionWatt = Objects.requireNonNullElse(dto.minProductionWatt(), DEFAULT_MIN_PRODUCTION_WATT);
        var newMaxProductionWatt = Objects.requireNonNullElse(dto.maxProductionWatt(), DEFAULT_MAX_PRODUCTION_WATT);

        validateRange("consumption", newMinConsumptionWatt, newMaxConsumptionWatt);
        validateRange("production", newMinProductionWatt, newMaxProductionWatt);

        this.minConsumptionWatt = newMinConsumptionWatt;
        this.maxConsumptionWatt = newMaxConsumptionWatt;
        this.minProductionWatt = newMinProductionWatt;
        this.maxProductionWatt = newMaxProductionWatt;
    }

    private static void validateRange(String direction, long min, long max) {
        if (min > max) {
            throw new InvalidSimulationRangeException(direction, min, max);
        }
    }
}
