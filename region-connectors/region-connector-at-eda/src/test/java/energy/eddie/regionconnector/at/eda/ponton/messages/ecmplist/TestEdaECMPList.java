// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist;

import energy.eddie.regionconnector.at.eda.dto.energycommunity.EdaECMPList;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EnergyCommunityMeteringPointData;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static energy.eddie.regionconnector.at.eda.EdaRegionConnectorMetadata.AT_ZONE_ID;

public record TestEdaECMPList(String messageId, String conversationId, String ecId) implements EdaECMPList {
    @Override
    public Optional<ZonedDateTime> endDate(String meterId) {
        return Optional.empty();
    }

    @Override
    public String senderMessageAddress() {
        return "";
    }

    @Override
    public String receiverMessageAddress() {
        return "";
    }

    @Override
    public ZonedDateTime documentCreationDateTime() {
        return ZonedDateTime.now(AT_ZONE_ID);
    }

    @Override
    public List<EnergyCommunityMeteringPointData> mpListData() {
        return List.of();
    }

    @Override
    public Object getOriginal() {
        return null;
    }
}
