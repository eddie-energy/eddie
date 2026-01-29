// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public interface Energy {

    @Nullable
    Granularity granularity();

    List<EnergyData> energyData();

    ZonedDateTime meterReadingStart();

    ZonedDateTime meterReadingEnd();

    String meteringReason();
}
