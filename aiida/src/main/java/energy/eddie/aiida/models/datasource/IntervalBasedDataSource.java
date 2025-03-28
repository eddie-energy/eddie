package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public class IntervalBasedDataSource extends DataSource {

    @JsonProperty
    @Column(name = "polling_interval")
    protected Integer pollingInterval;

    protected IntervalBasedDataSource() {
        this.pollingInterval = 5;
    }

    protected IntervalBasedDataSource(DataSourceDto dto, UUID userId) {
        super(dto, userId);
        this.pollingInterval = dto.simulationPeriod() != null ? dto.simulationPeriod() : 5;
    }

    public Integer pollingInterval() {
        return this.pollingInterval;
    }
}
