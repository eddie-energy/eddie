// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TerminationInteractionStep;

public class TerminationFollowingConstraint implements StructuralConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model current, Model next) {
        if (!(next instanceof TerminationInteractionStep)) {
            return new ConstraintOk();
        }
        if (current instanceof TerminationInteractionStep) {
            return new ConstraintViolation(
                    "TerminationInteractionStep must not be followed by another TerminationInteractionStep");
        }
        if (!(current instanceof StatusChangeStep statusChangeStep)) {
            return new ConstraintOk();
        }
        if (statusChangeStep.status() != PermissionProcessStatus.ACCEPTED) {
            return new ConstraintViolation("TerminationInteractionStep must follow an ACCEPTED StatusChangeStep");
        }
        return new ConstraintOk();
    }
}
