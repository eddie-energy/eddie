// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.cmnotification._01p12;

import at.ebutilities.schemata.customerconsent.cmnotification._01p12.CMNotification;
import energy.eddie.regionconnector.at.eda.dto.EdaCMNotification;
import energy.eddie.regionconnector.at.eda.dto.ResponseData;

import java.util.List;

public record EdaCMNotification01p12(CMNotification notification) implements EdaCMNotification {
    @Override
    public String conversationId() {
        return notification.getProcessDirectory().getConversationId();
    }

    @Override
    public String cmRequestId() {
        return notification.getProcessDirectory().getCMRequestId();
    }

    @Override
    public List<ResponseData> responseData() {
        return notification.getProcessDirectory().getResponseData().stream()
                           .map(responseData -> (ResponseData) new ResponseData01p12(responseData))
                           .toList();
    }
}
