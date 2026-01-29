// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;

public interface CMNotificationHandler {
    InboundMessageResult handle(
            EdaCMNotification cmNotification,
            NotificationMessageType notificationMessageType
    );
}
