package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public interface EdaCPNotification {
    String conversationId();

    String originalMessageId();

    List<Integer> responseCodes();
}
