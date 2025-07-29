package energy.eddie.regionconnector.at.eda.ponton.messages.consumptionrecord._01p41;

import energy.eddie.api.agnostic.Granularity;
import energy.eddie.regionconnector.at.eda.dto.Energy;
import energy.eddie.regionconnector.at.eda.dto.EnergyData;
import energy.eddie.regionconnector.at.eda.processing.utils.XmlGregorianCalenderUtils;

import java.time.ZonedDateTime;
import java.util.List;

public record Energy01p41(
        at.ebutilities.schemata.customerprocesses.consumptionrecord._01p41.Energy energy
) implements Energy {

    @Override
    public Granularity granularity() {
        return switch (energy.getMeteringIntervall()) {
            case QH -> Granularity.PT15M;
            case D -> Granularity.P1D;
            default ->
                    throw new IllegalStateException("Unexpected MeteringInterval value: '" + energy.getMeteringIntervall() + "'");
        };
    }

    @Override
    public List<EnergyData> energyData() {
        return energy.getEnergyData()
                     .stream()
                     .map(energyData -> (EnergyData) new EnergyData01p41(energyData))
                     .toList();
    }

    @Override
    public ZonedDateTime meterReadingStart() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodStart());
    }

    @Override
    public ZonedDateTime meterReadingEnd() {
        return XmlGregorianCalenderUtils.toUtcZonedDateTime(energy.getMeteringPeriodEnd());
    }

    @Override
    public String meteringReason() {
        return energy.getMeteringReason();
    }
}
