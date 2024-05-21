package energy.eddie.regionconnector.nl.mijn.aansluiting.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;

import java.util.function.Predicate;

import static energy.eddie.regionconnector.nl.mijn.aansluiting.MijnAansluitingRegionConnectorMetadata.SUPPORTED_ENERGY_TYPES;

public class SupportsEnergyTypePredicate implements Predicate<DataNeed> {
    @Override
    public boolean test(DataNeed dataNeed) {
        return !(dataNeed instanceof ValidatedHistoricalDataDataNeed vhd)
               || SUPPORTED_ENERGY_TYPES.contains(vhd.energyType());
    }
}
