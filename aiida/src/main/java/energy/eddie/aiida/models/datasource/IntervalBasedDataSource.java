package energy.eddie.aiida.models.datasource;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.IntervalBasedDataSourceDto;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public class IntervalBasedDataSource extends DataSource {
    private static final Integer DEFAULT_INTERVAL = 5;

    @JsonProperty
    @Column(name = "polling_interval")
    protected Integer pollingInterval;

    protected IntervalBasedDataSource() {
        this.pollingInterval = DEFAULT_INTERVAL;
    }

    protected IntervalBasedDataSource(IntervalBasedDataSourceDto dto, UUID userId) {
        super(dto, userId);
        applyDto(dto);
    }

    @Override
    public void update(DataSourceDto dto) {
        super.update(dto);
        if (dto instanceof IntervalBasedDataSourceDto intervalDto) {
            applyDto(intervalDto);
        }
    }

    private void applyDto(IntervalBasedDataSourceDto dto) {
        this.pollingInterval = dto.pollingInterval() != null ? dto.pollingInterval() : DEFAULT_INTERVAL;
    }

    public Integer pollingInterval() {
        return this.pollingInterval;
    }
}
