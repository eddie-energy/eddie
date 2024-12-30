package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.engine.SimulationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SequencedCollection;

public record Scenario(String name, List<Model> steps) implements Model {
    private static final Logger LOGGER = LoggerFactory.getLogger(Scenario.class);

    @Override
    public SequencedCollection<Step> execute(SimulationContext ctx) {
        LOGGER.debug("Starting scenario {}", name);
        return List.copyOf(steps);
    }
}
