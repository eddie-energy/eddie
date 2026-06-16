// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintResult;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TerminationInteractionStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TerminationFollowingConstraintTest {

    @Test
    void shouldReturnConstraintViolationForSuccessiveTerminationSteps() {
        // Given
        var constraint = new TerminationFollowingConstraint();
        Model model1 = new TerminationInteractionStep(Duration.ofSeconds(5));
        Model model2 = new TerminationInteractionStep(Duration.ofSeconds(10));

        // When
        ConstraintResult result = constraint.violatesConstraint(model1, model2);

        // Then
        assertThat(result).isInstanceOf(ConstraintViolation.class);
    }

    @Test
    void shouldReturnConstraintViolationForNonAcceptedStatus() {
        // Given
        var constraint = new TerminationFollowingConstraint();
        Model statusStep = new StatusChangeStep(PermissionProcessStatus.VALIDATED);
        Model terminationStep = new TerminationInteractionStep(Duration.ofSeconds(15));

        // When
        ConstraintResult result = constraint.violatesConstraint(statusStep, terminationStep);

        // Then
        assertThat(result).isInstanceOf(ConstraintViolation.class);
    }

    @Test
    void shouldReturnConstraintOkForAcceptedStatus() {
        // Given
        var constraint = new TerminationFollowingConstraint();
        Model acceptedStatusStep = new StatusChangeStep(PermissionProcessStatus.ACCEPTED);
        Model terminationStep = new TerminationInteractionStep(Duration.ofSeconds(10));

        // When
        ConstraintResult result = constraint.violatesConstraint(acceptedStatusStep, terminationStep);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }

    @Test
    void shouldReturnConstraintOkForUnrelatedModels() {
        // Given
        var constraint = new TerminationFollowingConstraint();
        Model unrelatedModel1 = new StatusChangeStep(PermissionProcessStatus.ACCEPTED);
        Model unrelatedModel2 = new StatusChangeStep(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR);

        // When
        ConstraintResult result = constraint.violatesConstraint(unrelatedModel1, unrelatedModel2);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }

    @Test
    void shouldReturnConstraintOkForUnrelatedPreviousStep() {
        // Given
        var constraint = new TerminationFollowingConstraint();
        Model previous = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData(
                        "",
                        ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
                        "",
                        List.of()
                )
        );
        Model next = new TerminationInteractionStep(Duration.ofSeconds(10));

        // When
        ConstraintResult result = constraint.violatesConstraint(previous, next);

        // Then
        assertThat(result).isInstanceOf(ConstraintOk.class);
    }
}