// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.at.eda.ponton.messages.masterdata._01p32;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.masterdata.MeteringPointData;
import energy.eddie.regionconnector.at.eda.xml.helper.EnergyDirection;
import jakarta.annotation.Nullable;

public record MeteringPointData01p32(
        at.ebutilities.schemata.customerprocesses.masterdata._01p32.MeteringPointData meteringPointData) implements MeteringPointData {

    @Override
    @Nullable
    public String supStatus() {
        var supStatus = meteringPointData.getSupStatus();
        return supStatus == null ? null : supStatus.value();
    }

    @Override
    @Nullable
    public String dsoTariff() {
        var dsoTariffClass = meteringPointData.getDSOTariffClass();
        return dsoTariffClass == null ? null : dsoTariffClass.getValue().value();
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
        var energyCommunity = meteringPointData.getEnergyCommunity();
        return energyCommunity == null ? null : energyCommunity.getValue().value();
    }

    @Override
    @Nullable
    public String typeOfGeneration() {
        var typeOfGeneration = meteringPointData.getTypeOfGeneration();
        return typeOfGeneration == null ? null : typeOfGeneration.getValue().value();
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
        return switch (deviceType.getValue()) {
            case IME -> Granularity.PT15M;
            default -> Granularity.P1D;
        };
    }
}
