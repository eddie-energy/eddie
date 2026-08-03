// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.cim.agnostic.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.LoadProfileCurveStep;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class DataFollowsAcceptedStepOrDataConstraintTest {
    private final DataFollowsAcceptedStepOrDataConstraint dataFollowsAcceptedStepOrDataConstraint = new DataFollowsAcceptedStepOrDataConstraint();

    @Test
    void testConstraint_whereCurrentIsNotAcceptedStep_returnsOk() {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);
        var next = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @ParameterizedTest
    @MethodSource("stepAndErrorMessageSource")
    void testConstraint_whereCurrentIsNotAcceptedAndNextIsDataStep_returnsViolation(
            Model current,
            Model next,
            String errorMessage
    ) {
        // Given

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals(errorMessage, violation.message());
    }

    @ParameterizedTest
    @MethodSource("stepSource")
    void testConstraint_whereCurrentIsAccepted_andNextDataStep_returnsOk(Model next) {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.ACCEPTED, 0);

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


    @Test
    void testConstraint_whereCurrentLoadProfileCurveStepStepAndNextStatusChangeStep_returnsOk() {
        // Given
        var current = new LoadProfileCurveStep(null, "default", 10, "mid");
        var next = new StatusChangeStep(PermissionProcessStatus.FULFILLED);

        // When
        var res = dataFollowsAcceptedStepOrDataConstraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }


    private static Stream<Arguments> stepSource() {
        return Stream.of(
                Arguments.of(getValidatedHistoricalDataStep()),
                Arguments.of(new LoadProfileCurveStep(null, "default", 10, "mid"))
        );
    }

    private static Stream<Arguments> stepAndErrorMessageSource() {
        var current = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);
        var loadProfileCurveStep = new LoadProfileCurveStep(null, "default", 10, "mid");
        return Stream.of(
                Arguments.of(current,
                             getValidatedHistoricalDataStep(),
                             "ValidatedHistoricalDataStep must follow ACCEPTED StatusChangeStep or another ValidatedHistoricalDataStep"),
                Arguments.of(current,
                             loadProfileCurveStep,
                             "LoadProfileCurveStep must follow ACCEPTED StatusChangeStep"),
                Arguments.of(loadProfileCurveStep,
                             loadProfileCurveStep,
                             "LoadProfileCurveStep must follow ACCEPTED StatusChangeStep")
        );
    }

    private static ValidatedHistoricalDataStep getValidatedHistoricalDataStep() {
        return new ValidatedHistoricalDataStep(
                new SimulatedValidatedHistoricalData("mid",
                                                     Optional.of(ZonedDateTime.now(ZoneOffset.UTC)),
                                                     Duration.ofMinutes(15),
                                                     List.of())
        );
    }
}