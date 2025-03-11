package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(InboundAiidaLocalDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class InboundAiidaLocalDataNeed extends AiidaLocalDataNeed {
    public static final String DISCRIMINATOR_VALUE = "inbound-aiida";

    @SuppressWarnings("NullAway.Init")
    protected InboundAiidaLocalDataNeed() {
    }

    public InboundAiidaLocalDataNeed(PermissionDetailsDto details) {
        super(details);
    }
}
