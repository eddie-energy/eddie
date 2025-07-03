package energy.eddie.aiida.models.permission.dataneed;

import energy.eddie.dataneeds.needs.aiida.AiidaDataNeed;
import energy.eddie.dataneeds.needs.aiida.InboundAiidaDataNeed;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(InboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class InboundAiidaLocalDataNeed extends AiidaLocalDataNeed {
    @SuppressWarnings("NullAway.Init")
    protected InboundAiidaLocalDataNeed() {
    }

    public InboundAiidaLocalDataNeed(AiidaDataNeed dataNeed) {
        super(dataNeed);
    }
}
