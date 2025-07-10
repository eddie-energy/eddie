package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p33;

import at.ebutilities.schemata.customerprocesses.masterdata._01p33.MasterData;
import energy.eddie.regionconnector.at.eda.dto.EdaMasterData;
import energy.eddie.regionconnector.at.eda.ponton.messages.masterdata.EdaMasterDataInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
public class EdaMasterData01p33InboundMessageFactory implements EdaMasterDataInboundMessageFactory {
    /**
     * The active from date of the message. The message is active from this date.
     * <p>From <a href="https://www.ebutilities.at/schemas/234">ebutilities</a>
     */
    private static final LocalDate ACTIVE_FROM = LocalDate.of(2025, 4, 7);
    private final Jaxb2Marshaller marshaller;

    public EdaMasterData01p33InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return !ACTIVE_FROM.isAfter(date);
    }

    @Override
    public EdaMasterData parseInputStream(InputStream inputStream) {
        var masterData = (MasterData) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaMasterData01p33(masterData);
    }
}
