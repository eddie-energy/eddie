package energy.eddie.dataneeds.needs.aiida;

/**
 * A data need designed to be fulfilled by an AIIDA instance by sending all data available it can get from the smart
 * meter.
 */
public class SmartMeterAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "smartMeterAiida";

    @SuppressWarnings("NullAway.Init")
    protected SmartMeterAiidaDataNeed() {
    }
}
