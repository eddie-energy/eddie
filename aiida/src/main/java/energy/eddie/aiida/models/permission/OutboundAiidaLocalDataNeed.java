package energy.eddie.aiida.models.permission;

import energy.eddie.aiida.dtos.PermissionDetailsDto;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(OutboundAiidaLocalDataNeed.DISCRIMINATOR_VALUE)
@SuppressWarnings("NullAway")
public class OutboundAiidaLocalDataNeed extends AiidaLocalDataNeed {
    public static final String DISCRIMINATOR_VALUE = "outbound-aiida";

    @SuppressWarnings("NullAway.Init")
    protected OutboundAiidaLocalDataNeed() {
    }

    public OutboundAiidaLocalDataNeed(PermissionDetailsDto details) {
        super(details);
    }
}
