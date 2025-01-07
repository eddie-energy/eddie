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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatusChangeStepConstraintTest {
    private final StatusChangeStepConstraint constraint = new StatusChangeStepConstraint();

    @Test
    void testConstraint_invalidDelay_returnsViolation() {
        // Given
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED, -10);

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Delay can only be 0 or more", violation.message());
    }

    @Test
    void testConstraint_validDelay_returnsOk() {
        // Given
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED, 10);

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_onNonStatusChangeStep_returnsOk() {
        // Given
        var step = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData(
                        "mid",
                        ZonedDateTime.now(ZoneOffset.UTC),
                        "PT15M",
                        List.of()
                )
        );

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }
}