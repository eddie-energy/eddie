package energy.eddie.aiida.models.datasource.mqtt.at;

import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_METER_ADAPTER)
public class OesterreichsEnergieDataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected OesterreichsEnergieDataSource() {}

    public OesterreichsEnergieDataSource(OesterreichsEnergieDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }
}
