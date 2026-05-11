// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p10;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ECMPList;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPListData;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EdaECMPList;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EnergyCommunityMeteringPointData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record ECMPList01p10(ECMPList ecmpList) implements EdaECMPList {
    @Override
    public String messageId() {
        return ecmpList.getProcessDirectory().getMessageId();
    }

    @Override
    public String conversationId() {
        return ecmpList.getProcessDirectory().getConversationId();
    }

    @Override
    public String ecId() {
        return ecmpList.getProcessDirectory().getECID();
    }

    @Override
    public Optional<ZonedDateTime> endDate(String meterId) {
        for (MPListData mp : ecmpList.getProcessDirectory()
                                     .getMPListData()) {
            if (mp.getMeteringPoint().equals(meterId)) {
                ZonedDateTime lastEnd = null;
                for (var mpTimeDatum : mp.getMPTimeData()) {
                    var current = XmlGregorianCalenderUtils.toUtcZonedDateTime(mpTimeDatum.getDateTo());
                    if (lastEnd == null || current.isAfter(lastEnd)) {
                        lastEnd = current;
                    }
                }
                return Optional.ofNullable(lastEnd);
            }
        }
        return Optional.empty();
    }

    @Override
    public String senderMessageAddress() {
        return ecmpList.getMarketParticipantDirectory().getRoutingHeader().getSender().getMessageAddress();
    }

    @Override
    public String receiverMessageAddress() {
        return ecmpList.getMarketParticipantDirectory().getRoutingHeader().getReceiver().getMessageAddress();
    }

    @Override
    public ZonedDateTime documentCreationDateTime() {
        return ecmpList.getMarketParticipantDirectory()
                       .getRoutingHeader()
                       .getDocumentCreationDateTime()
                       .toGregorianCalendar()
                       .toZonedDateTime();
    }

    @Override
    public List<EnergyCommunityMeteringPointData> mpListData() {
        List<EnergyCommunityMeteringPointData> list = new ArrayList<>();
        for (MPListData mpListData : ecmpList.getProcessDirectory().getMPListData()) {
            list.add(new EnergyCommunityMeteringPointData01p10(mpListData));
        }
        return list;
    }

    @Override
    public ECMPList getOriginal() {
        return ecmpList;
    }
}
