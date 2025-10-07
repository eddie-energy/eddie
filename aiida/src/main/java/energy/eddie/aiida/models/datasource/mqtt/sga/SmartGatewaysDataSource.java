package energy.eddie.aiida.models.datasource.mqtt.sga;

import energy.eddie.aiida.dtos.datasource.mqtt.sga.SmartGatewaysDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import energy.eddie.aiida.models.datasource.mqtt.SecretGenerator;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_GATEWAYS_ADAPTER)
public class SmartGatewaysDataSource extends MqttDataSource {
    private static final String TOPIC_SUFFIX = "/dsmr/reading/+";

    @SuppressWarnings("NullAway")
    protected SmartGatewaysDataSource() {}

    public SmartGatewaysDataSource(SmartGatewaysDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    protected void updateMqttSubscribeTopic() {
        this.mqttSubscribeTopic = SecretGenerator.generate() + TOPIC_SUFFIX;
    }
}
