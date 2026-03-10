// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist;

import energy.eddie.regionconnector.at.eda.dto.EdaECMPList;

import java.time.ZonedDateTime;
import java.util.Optional;

public record TestEdaECMPList(String messageId, String conversationId, String ecId) implements EdaECMPList {
    @Override
    public Optional<ZonedDateTime> endDate(String meterId) {
        return Optional.empty();
    }

    @Override
    public Object getOriginal() {
        return null;
    }
}
