// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.LoadProfileCurveGenerationStep;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.DayOfWeek;
import java.util.*;

public class LoadProfileCurveStep extends Model {
    static final String DISCRIMINATOR_VALUE = "LoadProfileCurveStep";
    private final Map<DayOfWeek, String> profilesPerWeekday;
    private final String defaultProfile;
    private final double maxEnergy;
    private final String meteringPoint;

    public LoadProfileCurveStep(
            @Nullable
            Map<DayOfWeek, String> profilesPerWeekday,
            @NonNull
            String defaultProfile,
            double maxEnergy,
            String meteringPoint
    ) {
        super(DISCRIMINATOR_VALUE);
        this.profilesPerWeekday = Objects.requireNonNullElseGet(profilesPerWeekday, Map::of);
        this.defaultProfile = defaultProfile;
        this.maxEnergy = maxEnergy;
        this.meteringPoint = meteringPoint;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) throws ExecutionException {
        return List.of(new LoadProfileCurveGenerationStep(profilesPerWeekday, defaultProfile,
                                                          maxEnergy, meteringPoint));
    }

    public Set<String> allProfiles() {
        var profiles = new HashSet<>(profilesPerWeekday.values());
        profiles.add(defaultProfile);
        return profiles;
    }
}
