package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import energy.eddie.dataneeds.needs.aiida.OutboundAiidaDataNeed;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(OutboundAiidaDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaLocalDataNeed extends AiidaLocalDataNeed {
    @SuppressWarnings("NullAway.Init")
    protected OutboundAiidaLocalDataNeed() {
    }

    public OutboundAiidaLocalDataNeed(PermissionDetailsDto details) {
        super(details);
    }
}