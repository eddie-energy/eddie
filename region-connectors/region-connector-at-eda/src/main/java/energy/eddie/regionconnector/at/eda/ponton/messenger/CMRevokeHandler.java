package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.dto.EdaCMRevoke;

public interface CMRevokeHandler {
    InboundMessageResult handle(EdaCMRevoke cmRevoke);
}
