// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.interval.simulation;

import energy.eddie.aiida.dtos.datasource.simulation.SimulationDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.interval.IntervalDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SIMULATION)
public class SimulationDataSource extends IntervalDataSource {
    @SuppressWarnings("NullAway")
    protected SimulationDataSource() {}

    public SimulationDataSource(SimulationDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }
}
