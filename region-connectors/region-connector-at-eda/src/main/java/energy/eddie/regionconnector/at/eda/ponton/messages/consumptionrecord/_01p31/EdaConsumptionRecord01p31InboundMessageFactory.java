package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p31;

import at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.ConsumptionRecord;
import energy.eddie.regionconnector.at.eda.dto.EdaConsumptionRecord;
import energy.eddie.regionconnector.at.eda.ponton.messages.InactivePontonMessageFactoryException;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord.EdaConsumptionRecordInboundMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class EdaConsumptionRecord01p31InboundMessageFactory implements EdaConsumptionRecordInboundMessageFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaConsumptionRecord01p31InboundMessageFactory.class);
    /**
     * The date until which this message is active. After this date the message is not valid anymore.
     * <p>From <a href="https://www.ebutilities.at/schemas/149">ebutilities</a>
     */
    private static final LocalDate ACTIVE_UNTIL = LocalDate.of(2024, 4, 7);
    private final Jaxb2Marshaller marshaller;

    public EdaConsumptionRecord01p31InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
        if (!isActive(LocalDate.now(AT_ZONE_ID))) {
            var exception = new InactivePontonMessageFactoryException(EdaConsumptionRecord01p31InboundMessageFactory.class);
            LOGGER.atError()
                  .addArgument(exception::getMessage)
                  .log("{}", exception);
        }
    }

    @Override
    public boolean isActive(LocalDate date) {
        return ACTIVE_UNTIL.isAfter(date) || ACTIVE_UNTIL.isEqual(date);
    }

    @Override
    public EdaConsumptionRecord parseInputStream(InputStream inputStream) {
        var consumptionRecord = (ConsumptionRecord) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaConsumptionRecord01p31(consumptionRecord);
    }
}
