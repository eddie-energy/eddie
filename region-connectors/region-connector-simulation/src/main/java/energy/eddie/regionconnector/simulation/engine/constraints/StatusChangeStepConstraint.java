// SPDX-FileCopyrightText: 2024 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;

public class StatusChangeStepConstraint implements ElementConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model model) {
        if (!(model instanceof StatusChangeStep statusChangeStep) || statusChangeStep.delay() >= 0.0) {
            return new ConstraintOk();
        }
        return new ConstraintViolation("Delay can only be 0 or more");
    }
}
