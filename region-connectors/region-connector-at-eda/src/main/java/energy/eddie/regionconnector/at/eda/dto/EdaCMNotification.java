package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public interface EdaCMNotification {

    String conversationId();

    String cmRequestId();

    List<ResponseData> responseData();
}
