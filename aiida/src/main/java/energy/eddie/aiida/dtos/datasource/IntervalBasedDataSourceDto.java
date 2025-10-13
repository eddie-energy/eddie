package energy.eddie.aiida.dtos.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"NullAway.Init"})
public abstract class IntervalBasedDataSourceDto extends DataSourceDto {
    @JsonProperty
    protected Integer pollingInterval;

    public Integer pollingInterval() {
        return pollingInterval;
    }
}

