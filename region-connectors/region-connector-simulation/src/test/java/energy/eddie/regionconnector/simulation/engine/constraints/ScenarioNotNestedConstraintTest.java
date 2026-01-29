// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ScenarioNotNestedConstraintTest {
    @Test
    void testConstraint_onNestedScenario_returnsViolation() {
        // Given
        var constraint = new ScenarioNotNestedConstraint();
        var scenario = new Scenario(
                "test1",
                List.of(new Scenario("test2", List.of()))
        );

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Scenarios cannot be nested", violation.message());
    }

    @Test
    void testConstraint_onNonScenarioStep_returnsOk() {
        // Given
        var constraint = new ScenarioNotNestedConstraint();
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED, 0);

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_onNotNestedScenario_returnsOk() {
        // Given
        var constraint = new ScenarioNotNestedConstraint();
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED, 0);
        var scenario = new Scenario("test", List.of(step));

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }
}