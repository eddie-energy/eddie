// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WaitForTerminationStepTest {

    @Test
    void shouldReturnAnEmptyListIfNoTerminationIsSent() {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(0));
        var ctx = TestSimulationContext.create();

        // When
        var res = step.execute(ctx);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void shouldReturnAnEmptyListIfNoMatchingTerminationIsSent() {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(0));
        var ctx = TestSimulationContext.create();

        // When
        ctx.documentStreams().publish(UUID.randomUUID().toString());
        var res = step.execute(ctx);

        // Then
        assertThat(res).isEmpty();
    }

    @Test
    void shouldReturnListWithStatusEmissionIfTerminationMatches() {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(0));
        var ctx = TestSimulationContext.create();

        // When
        ctx.documentStreams().publish(ctx.permissionId());
        var res = step.execute(ctx);

        // Then
        assertThat(res)
                .singleElement()
                .isInstanceOf(StatusEmissionStep.class);
    }
}