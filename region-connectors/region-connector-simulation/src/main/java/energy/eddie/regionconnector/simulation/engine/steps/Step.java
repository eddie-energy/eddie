package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.regionconnector.simulation.engine.SimulationContext;

import java.util.SequencedCollection;

/**
 * Defines a single step in a scenario.
 * A step can be part of the model (text) or the runtime structure.
 */
public interface Step {
    /**
     * When a step is executed, it returns a list of steps, which should be executed next.
     * It can also execute any other arbitrary code in this method.
     *
     * @param ctx context that might be needed by the step
     * @return next steps to be executed
     */
    SequencedCollection<Step> execute(SimulationContext ctx);
}
