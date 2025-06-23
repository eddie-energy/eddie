package energy.eddie.dataneeds.needs.aiida;

import jakarta.persistence.Entity;

@Entity
@SuppressWarnings("NullAway")
public class OutboundAiidaDataNeed extends AiidaDataNeed {
    public static final String DISCRIMINATOR_VALUE = "outbound-aiida";

    @SuppressWarnings("NullAway.Init")
    protected OutboundAiidaDataNeed() {
    }
}