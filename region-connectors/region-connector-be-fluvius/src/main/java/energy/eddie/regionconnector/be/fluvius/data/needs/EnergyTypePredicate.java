package energy.eddie.regionconnector.be.fluvius.data.needs;

import energy.eddie.dataneeds.needs.DataNeed;
import energy.eddie.dataneeds.needs.ValidatedHistoricalDataDataNeed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;

import static energy.eddie.regionconnector.be.fluvius.FluviusRegionConnectorMetadata.SUPPORTED_ENERGY_TYPES;

public class EnergyTypePredicate implements Predicate<DataNeed> {
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyTypePredicate.class.getName());

    @Override
    public boolean test(DataNeed dataNeed) {
        if (!(dataNeed instanceof ValidatedHistoricalDataDataNeed vhd)) return true;
        var supportedEnergyType = SUPPORTED_ENERGY_TYPES.contains(vhd.energyType());
        if (!supportedEnergyType) {
            LOGGER.info("Fluvius region-connector does not support energy type {}", vhd.energyType());
        }
        return supportedEnergyType;
    }
}
