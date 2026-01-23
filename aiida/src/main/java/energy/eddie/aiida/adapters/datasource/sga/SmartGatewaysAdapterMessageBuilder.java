// SPDX-FileCopyrightText: 2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.aiida.adapters.datasource.sga;

@SuppressWarnings("NullAway")
public class SmartGatewaysAdapterMessageBuilder {
    private SmartGatewaysAdapterMessageField electricityEquipmentId;
    private SmartGatewaysAdapterMessageField gasEquipmentId;
    private SmartGatewaysAdapterMessageField electricityTariff;
    private SmartGatewaysAdapterMessageField electricityDeliveredTariff1;
    private SmartGatewaysAdapterMessageField electricityReturnedTariff1;
    private SmartGatewaysAdapterMessageField electricityDeliveredTariff2;
    private SmartGatewaysAdapterMessageField electricityReturnedTariff2;
    private SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff1;
    private SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff1;
    private SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff2;
    private SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff2;
    private SmartGatewaysAdapterMessageField powerCurrentlyDelivered;
    private SmartGatewaysAdapterMessageField powerCurrentlyReturned;
    private SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL1;
    private SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL2;
    private SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL3;
    private SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL1;
    private SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL2;
    private SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL3;
    private SmartGatewaysAdapterMessageField phaseVoltageL1;
    private SmartGatewaysAdapterMessageField phaseVoltageL2;
    private SmartGatewaysAdapterMessageField phaseVoltageL3;
    private SmartGatewaysAdapterMessageField phasePowerCurrentL1;
    private SmartGatewaysAdapterMessageField phasePowerCurrentL2;
    private SmartGatewaysAdapterMessageField phasePowerCurrentL3;

    public SmartGatewaysAdapterMessage build() {
        return new SmartGatewaysAdapterMessage(
                electricityEquipmentId,
                gasEquipmentId,
                electricityTariff,
                electricityDeliveredTariff1,
                electricityReturnedTariff1,
                electricityDeliveredTariff2,
                electricityReturnedTariff2,
                reactiveEnergyDeliveredTariff1,
                reactiveEnergyReturnedTariff1,
                reactiveEnergyDeliveredTariff2,
                reactiveEnergyReturnedTariff2,
                powerCurrentlyDelivered,
                powerCurrentlyReturned,
                phaseCurrentlyDeliveredL1,
                phaseCurrentlyDeliveredL2,
                phaseCurrentlyDeliveredL3,
                phaseCurrentlyReturnedL1,
                phaseCurrentlyReturnedL2,
                phaseCurrentlyReturnedL3,
                phaseVoltageL1,
                phaseVoltageL2,
                phaseVoltageL3,
                phasePowerCurrentL1,
                phasePowerCurrentL2,
                phasePowerCurrentL3
        );
    }

    public void setElectricityEquipmentId(SmartGatewaysAdapterMessageField field) {
        this.electricityEquipmentId = field;
    }

    public void setGasEquipmentId(SmartGatewaysAdapterMessageField field) {
        this.gasEquipmentId = field;
    }

    public void setElectricityTariff(SmartGatewaysAdapterMessageField electricityTariff) {
        this.electricityTariff = electricityTariff;
    }

    public void setElectricityDeliveredTariff1(SmartGatewaysAdapterMessageField electricityDeliveredTariff1) {
        this.electricityDeliveredTariff1 = electricityDeliveredTariff1;
    }

    public void setElectricityReturnedTariff1(SmartGatewaysAdapterMessageField electricityReturnedTariff1) {
        this.electricityReturnedTariff1 = electricityReturnedTariff1;
    }

    public void setElectricityDeliveredTariff2(SmartGatewaysAdapterMessageField electricityDeliveredTariff2) {
        this.electricityDeliveredTariff2 = electricityDeliveredTariff2;
    }

    public void setElectricityReturnedTariff2(SmartGatewaysAdapterMessageField electricityReturnedTariff2) {
        this.electricityReturnedTariff2 = electricityReturnedTariff2;
    }

    public void setReactiveEnergyDeliveredTariff1(SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff1) {
        this.reactiveEnergyDeliveredTariff1 = reactiveEnergyDeliveredTariff1;
    }

    public void setReactiveEnergyReturnedTariff1(SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff1) {
        this.reactiveEnergyReturnedTariff1 = reactiveEnergyReturnedTariff1;
    }

    public void setReactiveEnergyDeliveredTariff2(SmartGatewaysAdapterMessageField reactiveEnergyDeliveredTariff2) {
        this.reactiveEnergyDeliveredTariff2 = reactiveEnergyDeliveredTariff2;
    }

    public void setReactiveEnergyReturnedTariff2(SmartGatewaysAdapterMessageField reactiveEnergyReturnedTariff2) {
        this.reactiveEnergyReturnedTariff2 = reactiveEnergyReturnedTariff2;
    }

    public void setPowerCurrentlyDelivered(SmartGatewaysAdapterMessageField powerCurrentlyDelivered) {
        this.powerCurrentlyDelivered = powerCurrentlyDelivered;
    }

    public void setPowerCurrentlyReturned(SmartGatewaysAdapterMessageField powerCurrentlyReturned) {
        this.powerCurrentlyReturned = powerCurrentlyReturned;
    }

    public void setPhaseCurrentlyDeliveredL1(SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL1) {
        this.phaseCurrentlyDeliveredL1 = phaseCurrentlyDeliveredL1;
    }

    public void setPhaseCurrentlyDeliveredL2(SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL2) {
        this.phaseCurrentlyDeliveredL2 = phaseCurrentlyDeliveredL2;
    }

    public void setPhaseCurrentlyDeliveredL3(SmartGatewaysAdapterMessageField phaseCurrentlyDeliveredL3) {
        this.phaseCurrentlyDeliveredL3 = phaseCurrentlyDeliveredL3;
    }

    public void setPhaseCurrentlyReturnedL1(SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL1) {
        this.phaseCurrentlyReturnedL1 = phaseCurrentlyReturnedL1;
    }

    public void setPhaseCurrentlyReturnedL2(SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL2) {
        this.phaseCurrentlyReturnedL2 = phaseCurrentlyReturnedL2;
    }

    public void setPhaseCurrentlyReturnedL3(SmartGatewaysAdapterMessageField phaseCurrentlyReturnedL3) {
        this.phaseCurrentlyReturnedL3 = phaseCurrentlyReturnedL3;
    }

    public void setPhaseVoltageL1(SmartGatewaysAdapterMessageField phaseVoltageL1) {
        this.phaseVoltageL1 = phaseVoltageL1;
    }

    public void setPhaseVoltageL2(SmartGatewaysAdapterMessageField phaseVoltageL2) {
        this.phaseVoltageL2 = phaseVoltageL2;
    }

    public void setPhaseVoltageL3(SmartGatewaysAdapterMessageField phaseVoltageL3) {
        this.phaseVoltageL3 = phaseVoltageL3;
    }

    public void setPhasePowerCurrentL1(SmartGatewaysAdapterMessageField phasePowerCurrentL1) {
        this.phasePowerCurrentL1 = phasePowerCurrentL1;
    }

    public void setPhasePowerCurrentL2(SmartGatewaysAdapterMessageField phasePowerCurrentL2) {
        this.phasePowerCurrentL2 = phasePowerCurrentL2;
    }

    public void setPhasePowerCurrentL3(SmartGatewaysAdapterMessageField phasePowerCurrentL3) {
        this.phasePowerCurrentL3 = phasePowerCurrentL3;
    }
}
