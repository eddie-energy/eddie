// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.regionconnector.simulation.engine.exceptions.ExecutionException;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;

class SimulationInterpret {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationInterpret.class);
    private final Scenario scenario;
    private final SimulationContext ctx;

    SimulationInterpret(Scenario scenario, SimulationContext ctx) {
        this.scenario = scenario;
        this.ctx = ctx;
    }


    void run() {
        LOGGER.info("Starting simulation scenario {}", scenario.name());
        try {
            executionLoop();
            LOGGER.info("Finishing simulation scenario {}", scenario.name());
        } catch (ExecutionException e) {
            LOGGER.info("Simulation was stopped by unsuccessful step", e);
        }
    }

    private void executionLoop() throws ExecutionException {
        final Deque<Step> stack = new ArrayDeque<>();
        stack.push(scenario);
        var stepCounter = 0;
        var start = System.nanoTime();
        while (!stack.isEmpty()) {
            stepCounter++;
            var item = stack.pop();
            LOGGER.debug("Executing step {}", item);
            var nextSteps = item.execute(ctx);
            LOGGER.debug("Pushing {} item(s) onto the stack", nextSteps.size());
            for (var nextStep : nextSteps.reversed()) {
                stack.push(nextStep);
            }
        }
        var end = System.nanoTime();
        LOGGER.atDebug()
              .addArgument(stepCounter)
              .addArgument(() -> Duration.of(end - start, ChronoUnit.NANOS).toString())
              .log("Simulation included {} steps and has been running for {}");
    }
}
