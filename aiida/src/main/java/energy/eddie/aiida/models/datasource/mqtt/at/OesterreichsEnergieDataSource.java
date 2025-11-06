package energy.eddie.aiida.models.datasource.mqtt.at;

import energy.eddie.aiida.dtos.datasource.mqtt.at.OesterreichsEnergieDataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttAccessControlEntry;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.SMART_METER_ADAPTER)
public class OesterreichsEnergieDataSource extends MqttDataSource {
    private static final String TOPIC_SUFFIX = "/data";

    @SuppressWarnings("NullAway")
    protected OesterreichsEnergieDataSource() {}

    public OesterreichsEnergieDataSource(OesterreichsEnergieDataSourceDto dto, UUID userId) {
        super(dto, userId);
    }

    @Override
    public void createAccessControlEntry() {
        var topic = TOPIC_PREFIX + id + TOPIC_SUFFIX;
        accessControlEntry = new MqttAccessControlEntry(id, topic);
    }
}
