package energy.eddie.aiida.models.datasource.simulation;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.IntervalBasedDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;


@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SIMULATION)
@SuppressWarnings("NullAway")
public class SimulationDataSource extends IntervalBasedDataSource {
    @SuppressWarnings("NullAway")
    protected SimulationDataSource() {}

    public SimulationDataSource(DataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    public DataSourceDto toDto() {
        return new DataSourceDto(
                id,
                dataSourceType,
                asset,
                name,
                enabled,
                simulationPeriod,
                null
        );
    }
}
