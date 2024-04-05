package energy.eddie.regionconnector.at.eda.dto;

import energy.eddie.api.agnostic.Granularity;

import java.time.ZonedDateTime;
import java.util.List;

public interface Energy {

    Granularity granularity();

    List<EnergyData> energyData();

    ZonedDateTime meterReadingStart();

    ZonedDateTime meterReadingEnd();

    String meteringReason();
}
