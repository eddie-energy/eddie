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
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TerminationFollowedByConstraintTest {

    @Test
    void shouldReturnOkWhenCurrentIsNotTerminationInteractionStep() {
        // Given
        Model current = new StatusChangeStep(PermissionProcessStatus.VALIDATED);
        Model next = new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        var constraint = new TerminationFollowedByConstraint();

        // When
        ConstraintResult result = constraint.violatesConstraint(current, next);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }

    @Test
    void shouldReturnOkWhenNextIsNull() {
        // Given
        Model current = new TerminationInteractionStep(Duration.ofSeconds(10));
        var constraint = new TerminationFollowedByConstraint();

        // When
        ConstraintResult result = constraint.violatesConstraint(current, null);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }

    @Test
    void shouldReturnViolationWhenNextIsNotStatusChangeStep() {
        // Given
        Model current = new TerminationInteractionStep(Duration.ofSeconds(10));
        Model next = new TerminationInteractionStep(Duration.ofSeconds(10));

        var constraint = new TerminationFollowedByConstraint();

        // When
        ConstraintResult result = constraint.violatesConstraint(current, next);

        // Then
        assertThat(result).isInstanceOf(ConstraintViolation.class);
    }

    @Test
    void shouldReturnViolationWhenNextStatusIsNotRequiresExternalTermination() {
        // Given
        Model current = new TerminationInteractionStep(Duration.ofSeconds(10));
        Model next = new StatusChangeStep(PermissionProcessStatus.ACCEPTED);
        var constraint = new TerminationFollowedByConstraint();

        // When
        ConstraintResult result = constraint.violatesConstraint(current, next);

        // Then
        assertThat(result).isInstanceOf(ConstraintViolation.class);
    }

    @Test
    void shouldReturnOkWhenNextStatusIsRequiresExternalTermination() {
        // Given
        Model current = new TerminationInteractionStep(Duration.ofSeconds(10));
        Model next = new StatusChangeStep(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION);
        var constraint = new TerminationFollowedByConstraint();

        // When
        ConstraintResult result = constraint.violatesConstraint(current, next);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }
}