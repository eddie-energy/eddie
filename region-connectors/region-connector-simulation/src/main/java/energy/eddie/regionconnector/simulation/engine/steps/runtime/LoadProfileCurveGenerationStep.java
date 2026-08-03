// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.agnostic.data.needs.ValidatedHistoricalDataDataNeedResult;
import energy.eddie.regionconnector.simulation.dtos.Measurement;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import energy.eddie.regionconnector.simulation.engine.steps.loadcurve.Profile;
import energy.eddie.regionconnector.simulation.engine.steps.loadcurve.StandardProfiles;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.*;

public class LoadProfileCurveGenerationStep implements Step {
    private final Map<DayOfWeek, Profile> profiles;
    private final double maxEnergy;
    private final String meteringPoint;

    public LoadProfileCurveGenerationStep(
            Map<DayOfWeek, String> profiles,
            String defaultProfile,
            double maxEnergy,
            String meteringPoint
    ) throws ExecutionException {
        this(profiles, defaultProfile, maxEnergy, meteringPoint, StandardProfiles.getInstance());
    }

    LoadProfileCurveGenerationStep(
            Map<DayOfWeek, String> profiles,
            String defaultProfile,
            double maxEnergy,
            String meteringPoint,
            StandardProfiles stdProfiles
    ) throws ExecutionException {
        this.maxEnergy = maxEnergy;
        this.meteringPoint = meteringPoint;
        this.profiles = initProfiles(profiles, defaultProfile, stdProfiles);
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) throws ExecutionException {
        if (!(ctx.calculationResult() instanceof ValidatedHistoricalDataDataNeedResult res)) {
            throw new ExecutionException("Invalid data need %s of type %s".formatted(ctx.dataNeed().id(),
                                                                                     ctx.dataNeed().type()));
        }
        var start = res.energyTimeframe().start().atStartOfDay(ZoneOffset.UTC);
        var end = res.energyTimeframe().end().plusDays(1).atStartOfDay(ZoneOffset.UTC);
        var minGranularity = res.granularities().getFirst().duration();
        var iterations = Duration.between(start, end).dividedBy(minGranularity);
        var measurements = new ArrayList<Measurement>();
        for (int i = 0; i < iterations; i++) {
            var loopingIndex = Duration.ofMinutes(i * minGranularity.toMinutes());
            var dow = start.plus(loopingIndex).getDayOfWeek();
            var profile = Objects.requireNonNull(profiles.get(dow));
            var energyValue = profile.get(loopingIndex, BigDecimal.valueOf(maxEnergy));
            measurements.add(
                    new Measurement(
                            energyValue.doubleValue(),
                            Measurement.MeasurementType.MEASURED
                    )
            );
        }
        return List.of(
                new ValidatedHistoricalDataEmissionStep(
                        new SimulatedValidatedHistoricalData(
                                meteringPoint,
                                start,
                                minGranularity,
                                measurements
                        )
                )
        );
    }

    private Map<DayOfWeek, Profile> initProfiles(
            Map<DayOfWeek, String> profiles,
            String defaultProfile,
            StandardProfiles stdProfiles
    ) throws ExecutionException {
        var finalProfiles = new EnumMap<DayOfWeek, Profile>(DayOfWeek.class);
        for (var dayOfWeek : DayOfWeek.values()) {
            var profile = profiles.getOrDefault(dayOfWeek, defaultProfile);
            finalProfiles.put(dayOfWeek,
                              stdProfiles.getProfile(profile)
                                         .orElseThrow(() -> new ExecutionException(profile + " is unknown")));
        }
        return finalProfiles;
    }
}
