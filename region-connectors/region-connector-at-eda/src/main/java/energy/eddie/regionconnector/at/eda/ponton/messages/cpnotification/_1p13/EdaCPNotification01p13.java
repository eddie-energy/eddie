package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification._1p13;

import at.ebutilities.schemata.customerprocesses.cpnotification._01p13.CPNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCPNotification;

import java.util.List;

public record EdaCPNotification01p13(CPNotification notification) implements EdaCPNotification {
    @Override
    public String conversationId() {
        return notification.getProcessDirectory().getConversationId();
    }

    @Override
    public String originalMessageId() {
        return notification.getProcessDirectory().getResponseData().getOriginalMessageID();
    }


    @Override
    public List<Integer> responseCodes() {
        return notification.getProcessDirectory().getResponseData().getResponseCode();
    }
}
