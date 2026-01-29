// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.masterdata.MeteringPointData;
import energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection;
import jakarta.annotation.Nullable;

public record NullMeteringPointData() implements MeteringPointData {
    @Nullable
    @Override
    public String supStatus() {
        return null;
    }

    @Nullable
    @Override
    public String dsoTariff() {
        return null;
    }

    @Nullable
    @Override
    public EnergyDirection energyDirection() {
        return null;
    }

    @Nullable
    @Override
    public String energyCommunity() {
        return null;
    }

    @Nullable
    @Override
    public String typeOfGeneration() {
        return null;
    }

    @Nullable
    @Override
    public String loadProfileType() {
        return null;
    }

    @Override
    public Granularity granularity() {
        return Granularity.P1D;
    }
}
