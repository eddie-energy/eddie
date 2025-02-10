package energy.eddie.aiida.datasources.simulation;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSource;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue("SIMULATION")
@SuppressWarnings("NullAway")
public class SimulationDataSource extends DataSource {
    @JsonProperty
    Integer simulationPeriod;

    public SimulationDataSource() {
    }

    public SimulationDataSource(String name, boolean enabled, UUID userId, AiidaAsset asset, DataSourceType dataSourceType, Integer simulationPeriod) {
        super(name, enabled, userId, asset, dataSourceType);
        this.simulationPeriod = simulationPeriod;
    }

    public Integer getSimulationPeriod() {
        return simulationPeriod;
    }

    public void setSimulationPeriod(Integer simulationPeriod) {
        this.simulationPeriod = simulationPeriod;
    }
}
