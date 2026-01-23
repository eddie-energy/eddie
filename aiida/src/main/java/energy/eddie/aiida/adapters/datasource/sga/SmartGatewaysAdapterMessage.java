// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import java.util.Objects;

public record SmartGatewaysAdapterMessage(
        SmartGatewaysAdapterMessageField electricityEquipmentId,
        SmartGatewaysAdapterMessageField gasEquipmentId,
        SmartGatewaysAdapterMessageField electricityTariff,
        SmartGatewaysAdapterMessageField electricityDeliveredTariff1,
        SmartGatewaysAdapterMessageField electricityReturnedTariff1,
        SmartGatewaysAdapterMessageField electricityDeliveredTariff2,
        SmartGatewaysAdapterMessageField electricityReturnedTariff2,
        SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff1,
        SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff1,
        SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff2,
        SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff2,
        SmartGatewaysAdapterMessageField powerCurrentlyDelivered,
        SmartGatewaysAdapterMessageField powerCurrentlyReturned,
        SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL1,
        SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL2,
        SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL3,
        SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL1,
        SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL2,
        SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL3,
        SmartGatewaysAdapterMessageField phaseVoltageL1,
        SmartGatewaysAdapterMessageField phaseVoltageL2,
        SmartGatewaysAdapterMessageField phaseVoltageL3,
        SmartGatewaysAdapterMessageField phasePowerCurrentL1,
        SmartGatewaysAdapterMessageField phasePowerCurrentL2,
        SmartGatewaysAdapterMessageField phasePowerCurrentL3
) {
    private static final String DSMR_TARIFF_LOW = "0001";

    SmartGatewaysAdapterMessageField electricityDelivered() {
        return Objects.equals(this.electricityTariff().value(), DSMR_TARIFF_LOW) ?
                this.electricityDeliveredTariff1() :
                this.electricityDeliveredTariff2();
    }

    SmartGatewaysAdapterMessageField electricityReturned() {
        return Objects.equals(this.electricityTariff().value(), DSMR_TARIFF_LOW) ?
                this.electricityReturnedTariff1() :
                this.electricityReturnedTariff2();
    }
}


