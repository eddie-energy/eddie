package energy.eddie.regionconnector.at.eda.ponton.messages.cmrequest;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CCMORequest;

public interface CMRequestOutboundMessageFactory extends PontonMessageFactory {
    OutboundMessage createOutboundMessage(CCMORequest ccmoRequest);
}
