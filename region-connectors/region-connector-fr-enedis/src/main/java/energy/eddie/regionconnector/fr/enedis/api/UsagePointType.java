package energy.eddie.regionconnector.fr.enedis.api;

import energy.eddie.regionconnector.fr.enedis.dto.contract.CustomerContract;
import energy.eddie.regionconnector.fr.enedis.dto.contract.UsagePointContract;
import org.apache.logging.log4j.util.Strings;

import java.util.Optional;

public enum UsagePointType {
    CONSUMPTION,
    PRODUCTION,
    CONSUMPTION_AND_PRODUCTION;

    public static Optional<UsagePointType> fromCustomerContract(CustomerContract contract) {
        boolean consumption = false;
        boolean production = false;

        for (UsagePointContract usagePointContract : contract.usagePointContracts()) {
            var segment = usagePointContract.contract().segment();
            if (Strings.isEmpty(segment)) {
                continue;
            }

            if (segment.contains("C")) {
                consumption = true;
            }

            if (segment.contains("P")) {
                production = true;
            }
        }
        return UsagePointType.fromBooleans(consumption, production);
    }

    public static Optional<UsagePointType> fromBooleans(boolean consumption, boolean production) {
        if (consumption && production) {
            return Optional.of(CONSUMPTION_AND_PRODUCTION);
        } else if (consumption) {
            return Optional.of(CONSUMPTION);
        } else if (production) {
            return Optional.of(PRODUCTION);
        } else {
            return Optional.empty();
        }
    }
}
