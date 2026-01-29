// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messenger;

import java.time.ZonedDateTime;

public interface MessengerMonitor {
    void resendFailedMessage(ZonedDateTime date, String messageId);
}
