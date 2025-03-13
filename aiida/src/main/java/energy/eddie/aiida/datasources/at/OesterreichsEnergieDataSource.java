package energy.eddie.aiida.datasources.at;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_METER_ADAPTER)
public class OesterreichsEnergieDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected OesterreichsEnergieDataSource() {}

    public OesterreichsEnergieDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId, dataSourceMqttDto);
    }
}
