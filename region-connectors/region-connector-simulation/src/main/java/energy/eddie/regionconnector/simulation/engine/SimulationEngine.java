// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.api.agnostic.data.needs.DataNeedCalculationService;
import energy.eddie.regionconnector.simulation.dtos.ScenarioMetadata;
import energy.eddie.regionconnector.simulation.engine.results.SimulationConstraintViolations;
import energy.eddie.regionconnector.simulation.engine.results.SimulationResult;
import energy.eddie.regionconnector.simulation.engine.results.SimulationStarted;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.providers.DocumentStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The simulation engine is used to execute test simulations.
 */
@Component
public class SimulationEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationEngine.class);
    private final DocumentStreams streams;
    private final DataNeedCalculationService calculationService;

    public SimulationEngine(
            DocumentStreams streams,
            DataNeedCalculationService calculationService
    ) {
        this.streams = streams;
        this.calculationService = calculationService;
    }

    public SimulationResult run(Scenario scenario, ScenarioMetadata metadata) {
        LOGGER.info("Checking scenario for constraint violations");
        var ctx = createSimulationContext(metadata);
        var constraints = new SimulationConstraints(scenario, ctx, calculationService);
        var violations = constraints.violatesConstraints();
        if (!violations.isEmpty()) {
            return new SimulationConstraintViolations(violations);
        }
        LOGGER.info("Running scenario");
        var interpret = new SimulationInterpret(scenario, ctx);
        var thread = Thread.startVirtualThread(interpret::run);
        return new SimulationStarted(thread);
    }

    private SimulationContext createSimulationContext(ScenarioMetadata metadata) {
        return new SimulationContext(streams,
                                     metadata.permissionId(),
                                     metadata.connectionId(),
                                     metadata.dataNeedId(),
                                     metadata.creationDateTime(),
                                     calculationService.calculate(metadata.dataNeedId(), metadata.creationDateTime()));
    }
}
