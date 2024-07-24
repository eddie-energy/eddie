package energy.eddie.regionconnector.at.eda.ponton.messenger;

import java.time.ZonedDateTime;

public interface MessengerMonitor {
    void resendFailedMessages(ZonedDateTime date);
}
