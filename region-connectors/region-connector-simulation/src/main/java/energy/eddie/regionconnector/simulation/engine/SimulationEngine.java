package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.dataneeds.services.DataNeedsService;
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
    private final DataNeedsService dataNeedsService;

    public SimulationEngine(
            DocumentStreams streams,
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") DataNeedsService dataNeedsService
    ) {
        this.streams = streams;
        this.dataNeedsService = dataNeedsService;
    }

    public SimulationResult run(Scenario scenario, ScenarioMetadata metadata) {
        LOGGER.info("Checking scenario for constraint violations");
        var ctx = createSimulationContext(metadata);
        var constraints = new SimulationConstraints(scenario, ctx, dataNeedsService);
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
                                     metadata.dataNeedId());
    }
}
