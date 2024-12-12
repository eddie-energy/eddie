package energy.eddie.aiida.datasources.sga;

public record SmartGatewaysAdapterMessage(
        SmartGatewaysMessageField electricityEquipmentId,
        SmartGatewaysMessageField gasEquipmentId,
        SmartGatewaysMessageField electricityTariff,
        SmartGatewaysMessageField electricityDeliveredTariff1,
        SmartGatewaysMessageField electricityReturnedTariff1,
        SmartGatewaysMessageField electricityDeliveredTariff2,
        SmartGatewaysMessageField electricityReturnedTariff2,
        SmartGatewaysMessageField reactiveEnergyDeliveredTariff1,
        SmartGatewaysMessageField reactiveEnergyReturnedTariff1,
        SmartGatewaysMessageField reactiveEnergyDeliveredTariff2,
        SmartGatewaysMessageField reactiveEnergyReturnedTariff2,
        SmartGatewaysMessageField powerCurrentlyDelivered,
        SmartGatewaysMessageField powerCurrentlyReturned,
        SmartGatewaysMessageField phaseCurrentlyDeliveredL1,
        SmartGatewaysMessageField phaseCurrentlyDeliveredL2,
        SmartGatewaysMessageField phaseCurrentlyDeliveredL3,
        SmartGatewaysMessageField phaseCurrentlyReturnedL1,
        SmartGatewaysMessageField phaseCurrentlyReturnedL2,
        SmartGatewaysMessageField phaseCurrentlyReturnedL3,
        SmartGatewaysMessageField phaseVoltageL1,
        SmartGatewaysMessageField phaseVoltageL2,
        SmartGatewaysMessageField phaseVoltageL3,
        SmartGatewaysMessageField phasePowerCurrentL1,
        SmartGatewaysMessageField phasePowerCurrentL2,
        SmartGatewaysMessageField phasePowerCurrentL3,
        SmartGatewaysMessageField gasDelivered,
        SmartGatewaysMessageField powerDeliveredHour,
        SmartGatewaysMessageField gasDeliveredHour
) {}


