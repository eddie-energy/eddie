package energy.eddie.aiida.datasources.sga;

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
        SmartGatewaysAdapterMessageField phasePowerCurrentL3,
        SmartGatewaysAdapterMessageField gasDelivered,
        SmartGatewaysAdapterMessageField powerDeliveredHour,
        SmartGatewaysAdapterMessageField gasDeliveredHour
) {}


