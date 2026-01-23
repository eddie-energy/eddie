// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.models.datasource.interval;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.datasource.DataSourceDto;
import energy.eddie.aiida.dtos.datasource.IntervalBasedDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SecondaryTable;

import java.util.UUID;

@Entity
@SecondaryTable(name = IntervalDataSource.TABLE_NAME, schema = "public")
public abstract class IntervalDataSource extends DataSource {
    public static final String TABLE_NAME = "data_source_interval";
    private static final Integer DEFAULT_INTERVAL = 5;

    @Column(name = "polling_interval", table = TABLE_NAME)
    @Schema(description = "Defines the interval the data source will poll for data.")
    @JsonProperty
    protected Integer pollingInterval;

    @SuppressWarnings("NullAway")
    protected IntervalDataSource() {
        this.pollingInterval = DEFAULT_INTERVAL;
    }

    protected IntervalDataSource(IntervalBasedDataSourceDto dto, UUID userId) {
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

    public Integer pollingInterval() {
        return this.pollingInterval;
    }

    private void applyDto(IntervalBasedDataSourceDto dto) {
        this.pollingInterval = dto.pollingInterval() != null ? dto.pollingInterval() : DEFAULT_INTERVAL;
    }
}
