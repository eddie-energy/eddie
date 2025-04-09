package energy.eddie.aiida.models.datasource.sga;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_GATEWAYS)
public class SmartGatewaysDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected SmartGatewaysDataSource() {}

    public SmartGatewaysDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId, dataSourceMqttDto);
    }
}
