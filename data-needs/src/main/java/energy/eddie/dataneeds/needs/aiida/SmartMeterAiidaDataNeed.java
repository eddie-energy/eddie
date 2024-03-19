package energy.eddie.dataneeds.needs.aiida;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A data need designed to be fulfilled by an AIIDA instance by sending all data available it can get from the smart
 * meter.
 */
@Entity
@Table(name = "smart_meter_aiida_data_need", schema = "data_needs")
@Schema(description = "Data need for an AIIDA instance to share all values it can read from the smart meter.")
public class SmartMeterAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "smartMeterAiida";

    @SuppressWarnings("NullAway.Init")
    protected SmartMeterAiidaDataNeed() {
    }
}
