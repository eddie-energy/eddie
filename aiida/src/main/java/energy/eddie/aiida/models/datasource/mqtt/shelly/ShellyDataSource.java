package energy.eddie.aiida.models.datasource.mqtt.shelly;

import energy.eddie.aiida.dtos.datasource.mqtt.shelly.ShellyDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SHELLY)
public class ShellyDataSource extends MqttDataSource {
    private static final String TOPIC_SUFFIX = "/#";
    @SuppressWarnings("NullAway")
    protected ShellyDataSource() {}

    public ShellyDataSource(ShellyDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
    }
}
