package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;

public interface ConsumptionRecordHandler {
    InboundMessageResult handle(EdaConsumptionRecord consumptionRecord);
}
