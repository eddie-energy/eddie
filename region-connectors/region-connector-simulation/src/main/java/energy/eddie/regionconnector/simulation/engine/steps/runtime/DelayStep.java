// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.Step;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.SequencedCollection;

public class DelayStep implements Step {
    private final Sleeper sleeper;
    private final long delay;
    private final ChronoUnit unit;

    public DelayStep(long delay, ChronoUnit unit) {
        this(
                delay,
                unit,
                Thread::sleep
        );
    }

    DelayStep(long delay, ChronoUnit unit, Sleeper sleeper) {
        this.sleeper = sleeper;
        this.delay = delay;
        this.unit = unit;
    }


    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        if (delay == 0.0) {
            return List.of();
        }
        try {
            sleeper.sleep(Duration.of(delay, unit));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return List.of();
    }

    @Override
    public int hashCode() {
        int result = sleeper.hashCode();
        result = 31 * result + Long.hashCode(delay);
        result = 31 * result + unit.hashCode();
        return result;
    }

    @Override
    public final boolean equals(Object o) {
        if (!(o instanceof DelayStep delayStep)) return false;

        return delay == delayStep.delay && sleeper.equals(delayStep.sleeper) && unit == delayStep.unit;
    }

    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }
}
