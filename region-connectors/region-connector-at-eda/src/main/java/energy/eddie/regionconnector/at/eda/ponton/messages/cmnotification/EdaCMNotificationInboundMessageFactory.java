// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification;

import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaCMNotificationInboundMessageFactory extends PontonMessageFactory {
    EdaCMNotification parseInputStream(InputStream inputStream);
}
