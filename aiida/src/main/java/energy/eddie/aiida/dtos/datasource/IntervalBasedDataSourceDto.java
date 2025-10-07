package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"NullAway.Init"})
public abstract class IntervalBasedDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected Integer simulationPeriod;

    public Integer simulationPeriod() {
        return simulationPeriod;
    }
}

