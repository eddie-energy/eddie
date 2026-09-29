// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p35;

import at.ebutilities.schemata.customerprocesses.masterdata._01p35.*;
import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.masterdata.MeteringPointData;
import energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection;
import jakarta.annotation.Nullable;

import java.util.Optional;

import static at.ebutilities.schemata.customerprocesses.masterdata._01p35.DeviceType.IME;

public record MeteringPointData01p35(
        at.ebutilities.schemata.customerprocesses.masterdata._01p35.MeteringPointData meteringPointData) implements MeteringPointData {

    @Override
    @Nullable
    public String supStatus() {
        var supStatus = meteringPointData.getSupStatus();
        return supStatus == null ? null : supStatus.value();
    }

    @Override
    @Nullable
    public String dsoTariff() {
        return Optional.ofNullable(meteringPointData.getElectricitySpecificData())
                       .map(ElectricitySpecificData::getDSOTariffClass)
                       .map(DSOTariffClassC::getValue)
                       .map(DSOTariffClass::value)
                       .orElse(null);
    }

    @Override
    public EnergyDirection energyDirection() {
        return switch (meteringPointData.getEnergyDirection()) {
            case CONSUMPTION -> EnergyDirection.CONSUMPTION;
            case GENERATION -> EnergyDirection.GENERATION;
        };
    }

    @Override
    @Nullable
    public String energyCommunity() {
        return Optional.ofNullable(meteringPointData.getElectricitySpecificData())
                       .map(ElectricitySpecificData::getEnergyCommunity)
                       .map(EnergyCommunityC::getValue)
                       .map(EnergyCommunity::value)
                       .orElse(null);
    }

    @Override
    @Nullable
    public String typeOfGeneration() {
        return Optional.ofNullable(meteringPointData.getElectricitySpecificData())
                       .map(ElectricitySpecificData::getTypeOfGeneration)
                       .map(TypeOfGenerationC::getValue)
                       .map(TypeOfGeneration::value)
                       .orElse(null);
    }

    @Override
    @Nullable
    public String loadProfileType() {
        var loadProfileType = meteringPointData.getLoadProfileType();
        return loadProfileType == null ? null : loadProfileType.getValue();
    }

    @Override
    public Granularity granularity() {
        var deviceType = meteringPointData.getDeviceType();
        if (deviceType == null) {
            return Granularity.P1D;
        }
        return deviceType.getValue() == IME ? Granularity.PT15M : Granularity.P1D;
    }
}
