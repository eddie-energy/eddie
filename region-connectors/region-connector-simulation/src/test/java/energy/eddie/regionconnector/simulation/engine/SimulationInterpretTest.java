// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationInterpretTest {

    @Test
    void testRun_executesStepsInCorrectOrder() {
        // Given
        var ctx = TestSimulationContext.create();
        var streams = ctx.documentStreams();
        var scenario = new Scenario("Test", List.of(
                new StatusChangeStep(PermissionProcessStatus.CREATED),
                new StatusChangeStep(PermissionProcessStatus.VALIDATED),
                new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR)
        ));
        var interpret = new SimulationInterpret(scenario, ctx);

        // When
        interpret.run();

        // Then
        StepVerifier.create(streams.getConnectionStatusMessageStream())
                    .then(streams::close)
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.CREATED, csm.status()))
                    .assertNext(csm -> assertEquals(PermissionProcessStatus.VALIDATED, csm.status()))
                    .assertNext(csm -> assertEquals(
                            PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                            csm.status()
                    ))
                    .verifyComplete();
    }
}