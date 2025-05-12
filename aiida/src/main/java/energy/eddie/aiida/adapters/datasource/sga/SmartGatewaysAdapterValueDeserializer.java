package energy.eddie.aiida.adapters.datasource.sga;

import energy.eddie.aiida.models.datasource.mqtt.sga.SmartGatewaysTopic;
import energy.eddie.aiida.models.record.UnitOfMeasurement;
import energy.eddie.aiida.utils.ObisCode;

import java.util.Map;

public class SmartGatewaysAdapterValueDeserializer {
    protected SmartGatewaysAdapterValueDeserializer() {}

    public static SmartGatewaysAdapterMessage deserialize(Map<SmartGatewaysTopic, String> batch) {
        SmartGatewaysAdapterMessageBuilder builder = new SmartGatewaysAdapterMessageBuilder();

        for (var entry : batch.entrySet()) {
            SmartGatewaysTopic topic = entry.getKey();
            String value = entry.getValue();

            switch (topic) {
                case ELECTRICITY_EQUIPMENT_ID -> builder.setElectricityEquipmentId(
                        new SmartGatewaysAdapterMessageField("electricityEquipmentId", value, UnitOfMeasurement.NONE, ObisCode.UNKNOWN));
                case GAS_EQUIPMENT_ID -> builder.setGasEquipmentId(
                        new SmartGatewaysAdapterMessageField("gasEquipmentId", value, UnitOfMeasurement.NONE, ObisCode.UNKNOWN));
                case ELECTRICITY_TARIFF -> builder.setElectricityTariff(
                        new SmartGatewaysAdapterMessageField("electricityTariff", value, UnitOfMeasurement.NONE, ObisCode.UNKNOWN));
                case ELECTRICITY_DELIVERED_1 -> builder.setElectricityDeliveredTariff1(
                        new SmartGatewaysAdapterMessageField("electricityDeliveredTariff1", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.POSITIVE_ACTIVE_ENERGY));
                case ELECTRICITY_RETURNED_1 -> builder.setElectricityReturnedTariff1(
                        new SmartGatewaysAdapterMessageField("electricityReturnedTariff1", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.NEGATIVE_ACTIVE_ENERGY));
                case ELECTRICITY_DELIVERED_2 -> builder.setElectricityDeliveredTariff2(
                        new SmartGatewaysAdapterMessageField("electricityDeliveredTariff2", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.POSITIVE_ACTIVE_ENERGY));
                case ELECTRICITY_RETURNED_2 -> builder.setElectricityReturnedTariff2(
                        new SmartGatewaysAdapterMessageField("electricityReturnedTariff2", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.NEGATIVE_ACTIVE_ENERGY));
                case REACTIVE_ELECTRICITY_DELIVERED_1 -> builder.setReactiveEnergyDeliveredTariff1(
                        new SmartGatewaysAdapterMessageField("reactiveEnergyDeliveredTariff1", value, UnitOfMeasurement.KILO_WATT, ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER));
                case REACTIVE_ELECTRICITY_RETURNED_1 -> builder.setReactiveEnergyReturnedTariff1(
                        new SmartGatewaysAdapterMessageField("reactiveEnergyReturnedTariff1", value, UnitOfMeasurement.KILO_WATT, ObisCode.NEGATIVE_REACTIVE_INSTANTANEOUS_POWER));
                case REACTIVE_ELECTRICITY_DELIVERED_2 -> builder.setReactiveEnergyDeliveredTariff2(
                        new SmartGatewaysAdapterMessageField("reactiveEnergyDeliveredTariff2", value, UnitOfMeasurement.KILO_WATT, ObisCode.POSITIVE_REACTIVE_INSTANTANEOUS_POWER));
                case REACTIVE_ELECTRICITY_RETURNED_2 -> builder.setReactiveEnergyReturnedTariff2(
                        new SmartGatewaysAdapterMessageField("reactiveEnergyReturnedTariff2", value, UnitOfMeasurement.KILO_WATT, ObisCode.NEGATIVE_REACTIVE_INSTANTANEOUS_POWER));
                case ELECTRICITY_CURRENTLY_DELIVERED -> builder.setPowerCurrentlyDelivered(
                        new SmartGatewaysAdapterMessageField("powerCurrentlyDelivered", value, UnitOfMeasurement.KILO_WATT, ObisCode.POSITIVE_ACTIVE_INSTANTANEOUS_POWER));
                case ELECTRICITY_CURRENTLY_RETURNED -> builder.setPowerCurrentlyReturned(
                        new SmartGatewaysAdapterMessageField("powerCurrentlyReturned", value, UnitOfMeasurement.KILO_WATT, ObisCode.NEGATIVE_ACTIVE_INSTANTANEOUS_POWER));
                case PHASE_CURRENTLY_DELIVERED_L1 -> builder.setPhaseCurrentlyDeliveredL1(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyDeliveredL1", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_CURRENTLY_DELIVERED_L2 -> builder.setPhaseCurrentlyDeliveredL2(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyDeliveredL2", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_CURRENTLY_DELIVERED_L3 -> builder.setPhaseCurrentlyDeliveredL3(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyDeliveredL3", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_CURRENTLY_RETURNED_L1 -> builder.setPhaseCurrentlyReturnedL1(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyReturnedL1", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_CURRENTLY_RETURNED_L2 -> builder.setPhaseCurrentlyReturnedL2(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyReturnedL2", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_CURRENTLY_RETURNED_L3 -> builder.setPhaseCurrentlyReturnedL3(
                        new SmartGatewaysAdapterMessageField("phaseCurrentlyReturnedL3", value, UnitOfMeasurement.KILO_WATT_HOUR, ObisCode.UNKNOWN));
                case PHASE_VOLTAGE_L1 -> builder.setPhaseVoltageL1(
                        new SmartGatewaysAdapterMessageField("phaseVoltageL1", value, UnitOfMeasurement.VOLT, ObisCode.UNKNOWN));
                case PHASE_VOLTAGE_L2 -> builder.setPhaseVoltageL2(
                        new SmartGatewaysAdapterMessageField("phaseVoltageL2", value, UnitOfMeasurement.VOLT, ObisCode.UNKNOWN));
                case PHASE_VOLTAGE_L3 -> builder.setPhaseVoltageL3(
                        new SmartGatewaysAdapterMessageField("phaseVoltageL3", value, UnitOfMeasurement.VOLT, ObisCode.UNKNOWN));
                case PHASE_POWER_CURRENT_L1 -> builder.setPhasePowerCurrentL1(
                        new SmartGatewaysAdapterMessageField("phasePowerCurrentL1", value, UnitOfMeasurement.AMPERE, ObisCode.UNKNOWN));
                case PHASE_POWER_CURRENT_L2 -> builder.setPhasePowerCurrentL2(
                        new SmartGatewaysAdapterMessageField("phasePowerCurrentL2", value, UnitOfMeasurement.AMPERE, ObisCode.UNKNOWN));
                case PHASE_POWER_CURRENT_L3 -> builder.setPhasePowerCurrentL3(
                        new SmartGatewaysAdapterMessageField("phasePowerCurrentL3", value, UnitOfMeasurement.AMPERE, ObisCode.UNKNOWN));
            }
        }

        return builder.build();
    }
}
