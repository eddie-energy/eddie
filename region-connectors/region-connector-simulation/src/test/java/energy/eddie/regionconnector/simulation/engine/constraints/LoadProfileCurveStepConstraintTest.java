// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.LoadProfileCurveStep;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LoadProfileCurveStepConstraintTest {
    private final LoadProfileCurveStepConstraint constraint = new LoadProfileCurveStepConstraint();

    @Test
    void testForUnrelatedStep_doesNothing() {
        // Given
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED);

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }


    @Test
    void testForUnknownProfiles_returnsConstraintViolation() {
        // Given
        var step = new LoadProfileCurveStep(
                Map.of(DayOfWeek.MONDAY, "unknown", DayOfWeek.TUESDAY, "All daytime"),
                "unknownDefault",
                10,
                "mid"
        );

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals(
                "Profiles [unknownDefault, unknown] are unknown, available profiles are: [Midday peak, Late afternoon, Evening, Early morning & evening, Mid morning, All daytime, Midday trough]",
                violation.message()
        );
    }

    @Test
    void testForKnownProfiles_doesNothing() {
        // Given
        var step = new LoadProfileCurveStep(
                Map.of(DayOfWeek.MONDAY, "All daytime", DayOfWeek.TUESDAY, "All daytime"),
                "All daytime",
                10,
                "mid"
        );

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }
}