package energy.eddie.regionconnector.at.eda.ponton.messages.cprequest;

import de.ponton.xp.adapter.api.messages.OutboundMessage;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;
import energy.eddie.regionconnector.at.eda.requests.CPRequestCR;

public interface CPRequestOutboundMessageFactory extends PontonMessageFactory {
    OutboundMessage createOutboundMessage(CPRequestCR cpRequest);
}
