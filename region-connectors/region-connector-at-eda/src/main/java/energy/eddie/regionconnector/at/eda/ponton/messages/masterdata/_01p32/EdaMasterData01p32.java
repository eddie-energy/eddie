package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import at.ebutilities.schemata.customerprocesses.masterdata._01p32.MasterData;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;

public record EdaMasterData01p32(
        MasterData masterData) implements EdaMasterData {
    @Override
    public String conversationId() {
        return masterData.getProcessDirectory().getConversationId();
    }

    @Override
    public String meteringPoint() {
        return masterData.getProcessDirectory().getMeteringPoint();
    }

    @Override
    public Object originalMasterData() {
        return masterData;
    }
}
