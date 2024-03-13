package energy.eddie.dataneeds.needs.aiida;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A data need designed to be fulfilled by an AIIDA instance by sending all data available it can get from the smart
 * meter.
 */
@Entity
@Table(name = "smart_meter_aiida_data_need", schema = "data_needs")
public class SmartMeterAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "smartMeterAiida";

    @SuppressWarnings("NullAway.Init")
    protected SmartMeterAiidaDataNeed() {
    }
}
