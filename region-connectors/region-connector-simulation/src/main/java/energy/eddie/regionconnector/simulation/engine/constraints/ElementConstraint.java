package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.steps.Model;

public interface ElementConstraint {
    ConstraintResult violatesConstraint(Model model);
}
