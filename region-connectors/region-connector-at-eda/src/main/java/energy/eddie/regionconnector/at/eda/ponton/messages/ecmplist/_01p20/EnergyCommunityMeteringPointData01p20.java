// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.ecmplist._01p20;

import at.ebutilities.schemata.customerprocesses.ecmplist._01p20.MPListData;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.EnergyCommunityMeteringPointData;
import energy.eddie.regionconnector.at.eda.dto.energycommunity.MeteringPointTimeData;

import java.util.ArrayList;
import java.util.List;

public record EnergyCommunityMeteringPointData01p20(MPListData mpListData) implements EnergyCommunityMeteringPointData {
    @Override
    public String meteringPoint() {
        return mpListData.getMeteringPoint();
    }

    @Override
    public List<MeteringPointTimeData> timeData() {
        List<MeteringPointTimeData> list = new ArrayList<>();
        for (var item : mpListData.getMPTimeData()) {
            list.add(new MeteringPointTimeData01p20(item));
        }
        return list;
    }
}
