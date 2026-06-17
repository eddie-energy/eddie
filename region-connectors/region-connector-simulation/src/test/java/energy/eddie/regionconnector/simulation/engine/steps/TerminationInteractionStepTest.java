// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.engine.steps.runtime.WaitForTerminationStep;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TerminationInteractionStepTest {
    @Test
    void shouldReturnListWithWaitForTerminationStep() {
        // Given
        var step = new TerminationInteractionStep(Duration.ofSeconds(10));
        var ctx = TestSimulationContext.create();

        // When
        var res = step.execute(ctx);

        // Then
        assertThat(res)
                .singleElement()
                .isInstanceOf(WaitForTerminationStep.class);
    }
}