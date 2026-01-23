// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine;

import energy.eddie.dataneeds.services.DataNeedsService;
import energy.eddie.regionconnector.simulation.engine.constraints.*;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

class SimulationConstraints {
    private final Scenario scenario;
    private final List<StructuralConstraint> structuralConstraints = List.of(
            new PermissionProcessModelConstraint(),
            new DataFollowsAcceptedStepOrDataConstraint()
    );
    private final List<ElementConstraint> elementConstraints;

    SimulationConstraints(Scenario scenario, SimulationContext ctx, DataNeedsService dataNeedsService) {
        this.scenario = scenario;
        this.elementConstraints = List.of(
                new DataNeedConstraint(dataNeedsService, ctx),
                new ScenarioConstraint(),
                new ScenarioNotNestedConstraint(),
                new StatusChangeStepConstraint()
        );
    }

    List<ConstraintViolation> violatesConstraints() {
        var stack = new ArrayDeque<>(scenario.steps());
        stack.push(scenario);
        var violations = new ArrayList<ConstraintViolation>();
        while (!stack.isEmpty()) {
            var item = stack.pop();
            for (var constraint : elementConstraints) {
                if (constraint.violatesConstraint(item) instanceof ConstraintViolation violation) {
                    violations.add(violation);
                }
            }
            for (var constraint : structuralConstraints) {
                if (constraint.violatesConstraint(item, stack.peek()) instanceof ConstraintViolation violation) {
                    violations.add(violation);
                }
            }
        }
        return violations;
    }
}
