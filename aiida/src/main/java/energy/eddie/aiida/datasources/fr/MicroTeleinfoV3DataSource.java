package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.dtos.DataSourceDto;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue(DataSourceType.Identifiers.MICRO_TELEINFO_V3)
@SuppressWarnings("NullAway")
public class MicroTeleinfoV3DataSource extends MqttDataSource {
    @JsonProperty
    private String meteringId;

    @SuppressWarnings("NullAway")
    protected MicroTeleinfoV3DataSource() {}

    public MicroTeleinfoV3DataSource(
            DataSourceDto dto,
            UUID userId,
            String mqttServerUri,
            String mqttUsername,
            String mqttPassword
    ) {
        super(dto, userId, mqttServerUri, mqttUsername, mqttPassword);
        this.meteringId = dto.meteringId();

        this.mqttSubscribeTopic = this.mqttSubscribeTopic + "/" + dto.meteringId();
    }

    public String meteringId() {
        return meteringId;
    }
}
