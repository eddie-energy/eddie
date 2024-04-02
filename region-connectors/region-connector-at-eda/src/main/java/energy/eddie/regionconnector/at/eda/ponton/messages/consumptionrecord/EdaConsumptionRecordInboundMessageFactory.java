package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord;

import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaConsumptionRecordInboundMessageFactory extends PontonMessageFactory {
    EdaConsumptionRecord parseInputStream(InputStream inputStream);
}
