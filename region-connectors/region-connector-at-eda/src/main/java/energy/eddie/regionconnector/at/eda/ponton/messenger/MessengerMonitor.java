package energy.eddie.regionconnector.at.eda.ponton.messenger;

import java.time.ZonedDateTime;

public interface MessengerMonitor {
    void resendFailedMessage(ZonedDateTime date, String messageId);
}
