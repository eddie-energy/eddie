// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.api.v0.PermissionProcessStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScenarioTest {
    @Test
    void testExecute_returnsContainedSteps() {
        // Given
        List<Model> steps = List.of(
                new StatusChangeStep(PermissionProcessStatus.CREATED)
        );
        var scenario = new Scenario("test", steps);
        var ctx = TestSimulationContext.create();

        // When
        var res = scenario.execute(ctx);

        // Then
        assertEquals(res, steps);
    }
}