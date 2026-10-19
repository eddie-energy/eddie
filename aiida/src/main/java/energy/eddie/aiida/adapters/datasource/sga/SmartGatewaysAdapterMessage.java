// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.api.agnostic.aiida.ObisCode;
import energy.eddie.api.agnostic.aiida.UnitOfMeasurement;
import jakarta.annotation.Nullable;

import java.math.BigDecimal;

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
    @Nullable
    SmartGatewaysAdapterMessageField electricityDelivered() {
        return totalEnergy("electricityDelivered",
                           electricityDeliveredTariff1,
                           electricityDeliveredTariff2,
                           ObisCode.POSITIVE_ACTIVE_ENERGY);
    }

    @Nullable
    SmartGatewaysAdapterMessageField electricityReturned() {
        return totalEnergy("electricityReturned",
                           electricityReturnedTariff1,
                           electricityReturnedTariff2,
                           ObisCode.NEGATIVE_ACTIVE_ENERGY);
    }

    @Nullable
    private static SmartGatewaysAdapterMessageField totalEnergy(
            String rawTag,
            @Nullable SmartGatewaysAdapterMessageField tariff1,
            @Nullable SmartGatewaysAdapterMessageField tariff2,
            ObisCode obisCode
    ) {
        // A missing cumulative register is unknown, not zero.
        if (tariff1 == null || tariff2 == null) {
            return null;
        }
        var total = new BigDecimal(tariff1.value()).add(new BigDecimal(tariff2.value()));
        return new SmartGatewaysAdapterMessageField(rawTag,
                                                   total.toPlainString(),
                                                   UnitOfMeasurement.KILO_WATT_HOUR,
                                                   obisCode);
    }
}

