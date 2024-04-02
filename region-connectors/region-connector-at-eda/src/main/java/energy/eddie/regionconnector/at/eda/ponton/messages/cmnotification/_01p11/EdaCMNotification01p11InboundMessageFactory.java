package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p11;

import at.ebutilities.schemata.customerconsent.cmnotification._01p11.CMNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification.EdaCMNotificationInboundMessageFactory;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;

import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.time.LocalDate;

@Component
@SuppressWarnings("DuplicatedCode")
public class EdaCMNotification01p11InboundMessageFactory implements EdaCMNotificationInboundMessageFactory {
    private final Jaxb2Marshaller marshaller;

    public EdaCMNotification01p11InboundMessageFactory(Jaxb2Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public boolean isActive(LocalDate date) {
        return true;
    }

    @Override
    public EdaCMNotification parseInputStream(InputStream inputStream) {
        var notification = (CMNotification) marshaller.unmarshal(new StreamSource(inputStream));
        return new EdaCMNotification01p11(notification);
    }
}
