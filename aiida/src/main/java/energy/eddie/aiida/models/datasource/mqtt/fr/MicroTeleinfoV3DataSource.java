package energy.eddie.aiida.models.datasource.mqtt.fr;

import energy.eddie.aiida.dtos.datasource.mqtt.fr.MicroTeleinfoV3DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.mqtt.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.MICRO_TELEINFO)
@SuppressWarnings("NullAway")
public class MicroTeleinfoV3DataSource extends MqttDataSource {
    @SuppressWarnings("NullAway")
    protected MicroTeleinfoV3DataSource() {}

    public MicroTeleinfoV3DataSource(MicroTeleinfoV3DataSourceDto dto, UUID userId) {
        super(dto, userId);
    }
}
