// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;
import jakarta.annotation.Nullable;

import java.time.ZonedDateTime;
import java.util.List;

public record Energy01p41(
        at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.Energy energy
) implements Energy {

    @Override
    @Nullable
    public Granularity granularity() {
        return switch (energy.getMeteringIntervall()) {
            case QH -> Granularity.PT15M;
            case H -> Granularity.PT1H;
            case D -> Granularity.P1D;
            case V -> null;
        };
    }

    @Override
    public List<EnergyData> energyData() {
        return energy.getEnergyData()
                     .stream()
                     .map(energyData -> (EnergyData) new EnergyData01p41(energyData))
                     .toList();
    }

    @Override
    public ZonedDateTime meterReadingStart() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodStart());
    }

    @Override
    public ZonedDateTime meterReadingEnd() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodEnd());
    }

    @Override
    public String meteringReason() {
        return energy.getMeteringReason();
    }
}
