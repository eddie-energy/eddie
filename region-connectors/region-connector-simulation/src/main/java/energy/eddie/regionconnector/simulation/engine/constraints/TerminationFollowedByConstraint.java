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

public class TerminationFollowedByConstraint implements StructuralConstraint {
    @Override
    public ConstraintResult violatesConstraint(Model current, Model next) {
        if (!(current instanceof TerminationInteractionStep) || next == null) {
            return new ConstraintOk();
        }
        if (!(next instanceof StatusChangeStep statusChangeStep)) {
            return new ConstraintViolation("TerminationInteractionStep must be followed by a StatusChangeStep");
        }
        if (statusChangeStep.status() != PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION) {
            return new ConstraintViolation("StatusChangeStep must have REQUIRES_EXTERNAL_TERMINATION status");
        }
        return new ConstraintOk();
    }
}
