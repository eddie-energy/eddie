package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.steps.Model;

public interface StructuralConstraint {
    ConstraintResult violatesConstraint(Model current, Model next);
}
