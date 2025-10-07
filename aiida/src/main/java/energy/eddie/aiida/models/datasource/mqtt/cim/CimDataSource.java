package energy.eddie.aiida.models.datasource.mqtt.cim;

import energy.eddie.aiida.dtos.datasource.mqtt.cim.CimDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.CIM_ADAPTER)
public class CimDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected CimDataSource() {}

    public CimDataSource(CimDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }
}
