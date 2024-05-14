package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GranularityChoice {
    private static final Comparator<Granularity> GRANULARITY_COMPARATOR = Comparator.comparingInt(Granularity::minutes);
    private final List<Granularity> supportedGranularities;

    public GranularityChoice(List<Granularity> supportedGranularities) {
        this.supportedGranularities = supportedGranularities.stream()
                                                            .sorted(GRANULARITY_COMPARATOR)
                                                            .toList();
    }

    @Nullable
    public Granularity find(Granularity min, Granularity max) {
        for (Granularity granularity : supportedGranularities) {
            if (isBetween(granularity, min, max)) {
                return granularity;
            }
        }
        return null;
    }

    /**
     * Returns true if the minute granularity is between the min and max granularities. Is inclusive
     *
     * @param current the granularity that is checked
     * @param min     the lower acceptable granularity
     * @param max     the higher acceptable granularity
     * @return true if the current granularity is between the min and max, otherwise false
     */
    private static boolean isBetween(Granularity current, Granularity min, Granularity max) {
        var minutes = current.minutes();
        return min.minutes() <= minutes && minutes <= max.minutes();
    }

    public List<Granularity> findAll(Granularity min, Granularity max) {
        List<Granularity> list = new ArrayList<>();
        for (Granularity current : supportedGranularities) {
            // Read like: current is between min and max
            if (isBetween(current, min, max)) {
                list.add(current);
            }
        }
        return list;
    }
}
