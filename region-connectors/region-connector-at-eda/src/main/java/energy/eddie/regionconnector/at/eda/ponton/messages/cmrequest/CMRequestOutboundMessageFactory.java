package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;

import java.time.LocalDate;

public interface CMRequestOutboundMessageFactory {
    OutboundMessage createOutboundMessage(CCMORequest ccmoRequest);

    boolean isActive(LocalDate date);
}
