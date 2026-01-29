// SPDX-FileCopyrightText: 2024-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.Scenario;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ScenarioConstraintTest {

    private final ScenarioConstraint constraint = new ScenarioConstraint();

    public static Stream<Arguments> testConstraint_onInvalidStepType_returnsViolation() {
        var validatedHistoricalDataStep = new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData(
                        "Mid",
                        ZonedDateTime.now(ZoneOffset.UTC),
                        "PT15M",
                        List.of()
                )
        );
        return Stream.of(
                Arguments.of(new StatusChangeStep(PermissionProcessStatus.CREATED, 0),
                             validatedHistoricalDataStep),
                Arguments.of(validatedHistoricalDataStep,
                             new StatusChangeStep(PermissionProcessStatus.FULFILLED, 0))
        );
    }

    @Test
    void testConstraint_forNonScenarioSteps_returnsOk() {
        // Given
        var step = new StatusChangeStep(PermissionProcessStatus.ACCEPTED, 0);

        // When
        var res = constraint.violatesConstraint(step);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_firstIsNotCreatedStep_returnsViolation() {
        // Given
        var first = new StatusChangeStep(PermissionProcessStatus.ACCEPTED, 0);
        var last = new StatusChangeStep(PermissionProcessStatus.FULFILLED, 0);
        var scenario = new Scenario("test", List.of(first, last));

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("First step in scenario must be a StatusStep with created status", violation.message());
    }

    @Test
    void testConstraint_lastIsNotFinalStatus_returnsViolation() {
        // Given
        var first = new StatusChangeStep(PermissionProcessStatus.CREATED, 0);
        var last = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);
        var scenario = new Scenario("test", List.of(first, last));

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Last step in scenario must have a final status", violation.message());
    }

    @Test
    void testConstraint_onValidFirstAndLastStep_returnsOk() {
        // Given
        var first = new StatusChangeStep(PermissionProcessStatus.CREATED, 0);
        var last = new StatusChangeStep(PermissionProcessStatus.FULFILLED, 0);
        var scenario = new Scenario("test", List.of(first, last));

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @ParameterizedTest
    @MethodSource
    void testConstraint_onInvalidStepType_returnsViolation(Model first, Model last) {
        // Given
        var scenario = new Scenario("test", List.of(first, last));

        // When
        var res = constraint.violatesConstraint(scenario);

        // Then
        assertInstanceOf(ConstraintViolation.class, res);
    }
}