// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto.masterdata;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection;
import jakarta.annotation.Nullable;

public interface MeteringPointData {

    @Nullable
    String supStatus();

    @Nullable
    String dsoTariff();

    @Nullable
    EnergyDirection energyDirection();

    @Nullable
    String energyCommunity();

    @Nullable
    String typeOfGeneration();

    @Nullable
    String loadProfileType();

    Granularity granularity();
}
