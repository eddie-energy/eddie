package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.ConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
public class EdaConsumptionRecord01p41InboundMessageFactory implements EdaConsumptionRecordInboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/240">ebutilities</a>
     */
    private static final LocalDate ACTIVE_FROM = LocalDate.of(2025, 10, 6);
    private final Jaxb2Marshaller marshaller;

    public EdaConsumptionRecord01p41InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !ACTIVE_FROM.isAfter(date);
    }


    @Override
    public EdaConsumptionRecord parseInputStream(InputStream inputStream) {
        var consumptionRecord = (ConsumptionRecord) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaConsumptionRecord01p41(consumptionRecord);
    }
}
