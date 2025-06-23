package energy.eddie.dataneeds.needs.aiida;

import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway")
public class InboundAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "inbound-aiida";

    @SuppressWarnings("NullAway.Init")
    protected InboundAiidaDataNeed() {
    }
}