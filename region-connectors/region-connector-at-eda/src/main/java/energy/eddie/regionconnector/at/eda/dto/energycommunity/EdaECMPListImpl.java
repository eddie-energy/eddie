// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.energycommunity;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.ECMPList;
import at.ebutilities.schemata.customerprocesses.ecmplist._01p10.MPListData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public record EdaECMPListImpl(
        String messageId,
        String conversationId,
        String ecId,
        String senderMessageAddress,
        String receiverMessageAddress,
        ZonedDateTime documentCreationDateTime,
        List<EnergyCommunityMeteringPointData> mpListData,
        Object original
) implements EdaECMPList {

    @Override
    public Optional<ZonedDateTime> endDate(String meterId) {
        ECMPList ecmpList = (ECMPList) original;
        for (MPListData mp : ecmpList.getProcessDirectory().getMPListData()) {
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
    public Object getOriginal() {
        return original;
    }
}
