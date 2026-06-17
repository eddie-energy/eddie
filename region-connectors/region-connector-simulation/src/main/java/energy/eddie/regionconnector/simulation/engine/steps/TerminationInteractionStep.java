// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.WaitForTerminationStep;

import java.time.Duration;
import java.util.List;
import java.util.SequencedCollection;

public class TerminationInteractionStep extends Model {
    public static final String DISCRIMINATOR_VALUE = "TerminationInteractionStep";
    private final Duration waitFor;

    @JsonCreator
    public TerminationInteractionStep(@JsonProperty("waitFor") Duration waitFor) {
        super(DISCRIMINATOR_VALUE);
        this.waitFor = waitFor;
    }

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        return List.of(
                new WaitForTerminationStep(waitFor)
        );
    }
}
