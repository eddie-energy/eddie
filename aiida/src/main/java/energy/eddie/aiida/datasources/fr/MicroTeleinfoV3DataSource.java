package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.DataSourceType;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import energy.eddie.dataneeds.needs.aiida.AiidaAsset;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.UUID;

@Entity
@DiscriminatorValue("MICRO_TELEINFO_V3")
@SuppressWarnings("NullAway")
public class MicroTeleinfoV3DataSource extends MqttDataSource {
    @JsonProperty
    private String meteringId;

    public MicroTeleinfoV3DataSource() {
    }

    public MicroTeleinfoV3DataSource(String name, boolean enabled, UUID userId, AiidaAsset asset, DataSourceType dataSourceType, String mqttServerUri, String mqttSubscribeTopic, String mqttUsername, String mqttPassword, String meteringId) {
        super(name, enabled, userId, asset, dataSourceType, mqttServerUri, mqttSubscribeTopic, mqttUsername, mqttPassword);
        this.meteringId = meteringId;
    }

    public String getMeteringId() {
        return meteringId;
    }

    public void setMeteringId(String meteringId) {
        this.meteringId = meteringId;
    }
}
