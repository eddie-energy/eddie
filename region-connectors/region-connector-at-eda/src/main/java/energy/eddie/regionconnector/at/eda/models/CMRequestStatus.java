// SPDX-FileCopyrightText: 2023-2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.models;

import energy.eddie.regionconnector.at.eda.ponton.messenger.NotificationMessageType;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Optional;

public record CMRequestStatus(
        NotificationMessageType messageType,
        String conversationId,
        @Nullable String cmRequestId,
        List<ConsentData> consentData
) {
    public CMRequestStatus(NotificationMessageType messageType, String conversationId, String message) {
        this(messageType,
             conversationId,
             null,
             List.of(new ConsentData(List.of(), message, Optional.empty(), Optional.empty())));
    }

    public String message() {
        return consentData.stream()
                          .map(ConsentData::message)
                          .reduce((a, b) -> a + "; " + b)
                          .orElse("No response codes provided.");
    }
}
