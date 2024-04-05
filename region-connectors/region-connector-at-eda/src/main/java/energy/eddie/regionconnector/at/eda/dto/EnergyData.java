package energy.eddie.regionconnector.at.eda.dto;

import java.util.List;

public interface EnergyData {
    List<EnergyPosition> energyPositions();

    String meterCode();

    String billingUnit();
}
