package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification;

import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaCMNotificationInboundMessageFactory extends PontonMessageFactory {
    EdaCMNotification parseInputStream(InputStream inputStream);
}
