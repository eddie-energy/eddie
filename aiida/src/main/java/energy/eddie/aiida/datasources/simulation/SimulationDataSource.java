package energy.eddie.aiida.datasources.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;


@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SIMULATION)
@SuppressWarnings("NullAway")
public class SimulationDataSource extends DataSource {
    @JsonProperty
    private Integer simulationPeriod;

    @SuppressWarnings("NullAway")
    protected SimulationDataSource() {}

    public SimulationDataSource(DataSourceDto dto, UUID userId) {
        super(dto, userId);
        this.simulationPeriod = dto.simulationPeriod();
    }

    public Integer simulationPeriod() {
        return simulationPeriod;
    }

    @Override
    public DataSourceDto toDto() {
        return new DataSourceDto(
                id,
                dataSourceType.identifier(),
                asset.asset(),
                name,
                enabled,
                null,
                simulationPeriod,
                null
        );
    }
}
