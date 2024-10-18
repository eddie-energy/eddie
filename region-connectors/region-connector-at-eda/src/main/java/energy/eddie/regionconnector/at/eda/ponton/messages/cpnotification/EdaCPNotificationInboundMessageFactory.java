package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification;

import energy.eddie.regionconnector.at.eda.dto.EdaCPNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaCPNotificationInboundMessageFactory extends PontonMessageFactory {
    EdaCPNotification parseInputStream(InputStream inputStream);
}
