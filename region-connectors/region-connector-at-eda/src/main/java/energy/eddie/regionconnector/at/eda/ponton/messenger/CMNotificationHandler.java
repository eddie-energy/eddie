package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;

public interface CMNotificationHandler {
    InboundMessageResult handle(
            EdaCMNotification cmNotification,
            NotificationType notificationType
    );
}
