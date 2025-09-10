package energy.eddie.aiida.models.datasource.mqtt.shelly;

import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.dtos.DataSourceMqttDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SHELLY_EM)
public class ShellyEMDataSource extends MqttDataSource {
    private static final String TOPIC_SUFFIX = "/#";
    @SuppressWarnings("NullAway")
    protected ShellyEMDataSource() {}

    public ShellyEMDataSource(DataSourceDto dto, UUID userId, DataSourceMqttDto dataSourceMqttDto) {
        super(dto, userId, dataSourceMqttDto);
    }

    @Override
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
    }
}
