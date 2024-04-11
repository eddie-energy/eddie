package energy.eddie.regionconnector.at.eda.ponton.messenger;

import de.ponton.xp.adapter.api.domainvalues.InboundStatusEnum;

public record InboundMessageResult(InboundStatusEnum status, String statusMessage) {
}
