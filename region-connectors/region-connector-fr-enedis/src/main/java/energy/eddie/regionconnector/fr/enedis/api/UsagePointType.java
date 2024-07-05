package energy.eddie.regionconnector.fr.enedis.api;

import java.util.Optional;

public enum UsagePointType {
    CONSUMPTION,
    PRODUCTION,
    CONSUMPTION_AND_PRODUCTION;

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
