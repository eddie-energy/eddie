// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.LoadProfileCurveGenerationStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LoadProfileCurveStepTest {

    @Test
    void testCreationOfGenerationStep() throws ExecutionException {
        // Given
        var step = new LoadProfileCurveStep(null, "All daytime", 10, "mid");
        var ctx = TestSimulationContext.create();

        // When
        var res = step.execute(ctx);

        // Then
        assertInstanceOf(LoadProfileCurveGenerationStep.class, res.getFirst());
    }
}