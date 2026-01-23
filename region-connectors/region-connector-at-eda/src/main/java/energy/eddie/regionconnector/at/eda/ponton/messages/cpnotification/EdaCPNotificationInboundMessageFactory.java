// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cpnotification;

import energy.eddie.regionconnector.at.eda.dto.EdaCPNotification;
import energy.eddie.regionconnector.at.eda.ponton.messages.PontonMessageFactory;

import java.io.InputStream;

public interface EdaCPNotificationInboundMessageFactory extends PontonMessageFactory {
    EdaCPNotification parseInputStream(InputStream inputStream);
}
