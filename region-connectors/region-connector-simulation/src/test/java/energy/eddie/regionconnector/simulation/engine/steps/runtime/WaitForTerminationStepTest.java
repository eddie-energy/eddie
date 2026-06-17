// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WaitForTerminationStepTest {

    @Test
    void shouldThrowNoTerminationIsSent() {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(0));
        var ctx = TestSimulationContext.create();

        // When & Then
        assertThatThrownBy(() -> step.execute(ctx)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void shouldThrowIfNoMatchingTerminationIsSent() {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(0));
        var ctx = TestSimulationContext.create();

        // When & Then
        ctx.documentStreams().publish(UUID.randomUUID().toString());
        assertThatThrownBy(() -> step.execute(ctx)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void shouldReturnListWithStatusEmissionIfTerminationMatches() throws ExecutionException {
        // Given
        var step = new WaitForTerminationStep(Duration.ofSeconds(1));
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