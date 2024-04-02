package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p31;

import at.ebutilities.schemata.customerprocesses.masterdata._01p31.MasterData;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.ponton.messages.InactivePontonMessageFactoryException;
import energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p31.EdaConsumptionRecord01p31InboundMessageFactory;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.EdaMasterDataInboundMessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

@Component
public class EdaMasterData01p31InboundMessageFactory implements EdaMasterDataInboundMessageFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(EdaMasterData01p31InboundMessageFactory.class);

    /**
     * The date until which this message is active. After this date the message is not valid anymore.
     * <p>From <a href="https://www.ebutilities.at/schemas/158">ebutilities</a>
     */
    private static final LocalDate ACTIVE_UNTIL = LocalDate.of(2024, 4, 7);
    private final Jaxb2Marshaller marshaller;

    public EdaMasterData01p31InboundMessageFactory(Jaxb2Marshaller marshaller) {
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
    public EdaMasterData parseInputStream(InputStream inputStream) {
        var masterData = (MasterData) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaMasterData01p31(masterData);
    }
}
