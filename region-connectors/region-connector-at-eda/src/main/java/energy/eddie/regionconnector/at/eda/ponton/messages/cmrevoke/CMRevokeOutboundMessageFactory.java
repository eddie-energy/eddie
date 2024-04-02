package energy.eddie.regionconnector.at.eda.ponton.messages.cmrevoke;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.requests.CCMORevoke;

import java.time.LocalDate;

public interface CMRevokeOutboundMessageFactory {
    OutboundMessage createOutboundMessage(CCMORevoke ccmoRevoke);

    boolean isActive(LocalDate date);
}
