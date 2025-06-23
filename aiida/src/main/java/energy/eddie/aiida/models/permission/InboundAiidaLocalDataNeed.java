package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
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

    public InboundAiidaLocalDataNeed(PermissionDetailsDto details) {
        super(details);
    }
}
