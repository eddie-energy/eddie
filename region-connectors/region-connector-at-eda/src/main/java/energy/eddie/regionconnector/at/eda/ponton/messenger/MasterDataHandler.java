package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;

public interface MasterDataHandler {
    InboundMessageResult handle(EdaMasterData masterData);
}
