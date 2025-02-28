package energy.eddie.aiida.datasources.fr;

import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.aiida.models.datasource.MqttDataSource;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MICRO_TELEINFO_V3")
@SuppressWarnings("NullAway")
public class MicroTeleinfoV3DataSource extends MqttDataSource {
    @JsonProperty
    private String meteringId;

    public String getMeteringId() {
        return meteringId;
    }

    public void setMeteringId(String meteringId) {
        this.meteringId = meteringId;
    }
}
