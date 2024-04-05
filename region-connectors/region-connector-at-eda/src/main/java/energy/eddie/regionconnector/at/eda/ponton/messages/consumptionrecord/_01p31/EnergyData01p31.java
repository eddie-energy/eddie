package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p31;

import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.dto.EnergyPosition;

import java.util.List;

public record EnergyData01p31(
        at.ebutilities.schemata.customerprocesses.consumptionrecord._01p31.EnergyData energyData) implements EnergyData {

    @Override
    public List<EnergyPosition> energyPositions() {
        return energyData.getEP().stream()
                         .map(energyPosition -> new EnergyPosition(
                                 energyPosition.getBQ(),
                                 energyPosition.getMM()
                         ))
                         .toList();
    }

    @Override
    public String meterCode() {
        return energyData.getMeterCode();
    }

    @Override
    public String billingUnit() {
        return energyData.getUOM().value();
    }
}
