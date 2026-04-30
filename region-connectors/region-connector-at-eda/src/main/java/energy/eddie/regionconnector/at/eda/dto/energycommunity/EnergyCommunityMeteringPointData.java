// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.energycommunity;

import java.util.List;

public interface EnergyCommunityMeteringPointData {
    String meteringPoint();

    List<MeteringPointTimeData> timeData();
}
