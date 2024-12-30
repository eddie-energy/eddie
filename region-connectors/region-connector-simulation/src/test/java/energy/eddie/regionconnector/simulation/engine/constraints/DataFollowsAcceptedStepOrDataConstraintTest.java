package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataFollowsAcceptedStepOrDataConstraintTest {
    private final DataFollowsAcceptedStepOrDataConstraint dataFollowsAcceptedStepOrDataConstraint = new DataFollowsAcceptedStepOrDataConstraint();

    @Test
    void testConstraint_whereCurrentIsNotAcceptedStep_returnsOk() {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0, ChronoUnit.SECONDS);
        var next = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0, ChronoUnit.SECONDS);

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_whereCurrentIsNotAcceptedAndNextIsDataStep_returnsViolation() {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0, ChronoUnit.SECONDS);
        var next = getValidatedHistoricalDataStep();

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals(
                "ValidatedHistoricalDataStep must follow ACCEPTED StatusChangeStep or another ValidatedHistoricalDataStep",
                violation.message()
        );
    }

    @Test
    void testConstraint_whereCurrentIsAccepted_andNextDataStep_returnsOk() {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.ACCEPTED, 0, ChronoUnit.SECONDS);
        var next = getValidatedHistoricalDataStep();

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_whereCurrentAndNextAreDataSteps_returnsOk() {
        // Given
        var current = getValidatedHistoricalDataStep();
        var next = getValidatedHistoricalDataStep();

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    private static ValidatedHistoricalDataStep getValidatedHistoricalDataStep() {
        return new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mid", ZonedDateTime.now(ZoneOffset.UTC), "mid", List.of())
        );
    }
}