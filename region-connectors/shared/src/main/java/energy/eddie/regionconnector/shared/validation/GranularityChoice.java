package energy.eddie.regionconnector.shared.validation;

import energy.eddie.api.agnostic.Granularity;
import jakarta.annotation.Nullable;

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
            if (min.minutes() <= granularity.minutes() && granularity.minutes() <= max.minutes()) {
                return granularity;
            }
        }
        return null;
    }
}
