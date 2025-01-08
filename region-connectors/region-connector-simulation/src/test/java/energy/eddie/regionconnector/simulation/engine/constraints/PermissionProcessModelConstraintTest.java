package energy.eddie.regionconnector.simulation.engine.constraints;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintOk;
import energy.eddie.regionconnector.simulation.engine.constraints.results.ConstraintViolation;
import energy.eddie.regionconnector.simulation.engine.steps.Model;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.ValidatedHistoricalDataStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PermissionProcessModelConstraintTest {
    public static Stream<Arguments> testConstraint_onFollowUpState_returnsOk() {
        return Stream.of(
                Arguments.of(PermissionProcessStatus.CREATED, PermissionProcessStatus.VALIDATED),
                Arguments.of(PermissionProcessStatus.CREATED, PermissionProcessStatus.MALFORMED),
                Arguments.of(PermissionProcessStatus.VALIDATED,
                             PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR),
                Arguments.of(PermissionProcessStatus.VALIDATED, PermissionProcessStatus.UNABLE_TO_SEND),
                Arguments.of(PermissionProcessStatus.UNABLE_TO_SEND, PermissionProcessStatus.VALIDATED),
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR, PermissionProcessStatus.INVALID),
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                             PermissionProcessStatus.REJECTED),
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                             PermissionProcessStatus.TIMED_OUT),
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                             PermissionProcessStatus.ACCEPTED),
                Arguments.of(PermissionProcessStatus.SENT_TO_PERMISSION_ADMINISTRATOR,
                             PermissionProcessStatus.ACCEPTED),
                Arguments.of(PermissionProcessStatus.ACCEPTED, PermissionProcessStatus.REVOKED),
                Arguments.of(PermissionProcessStatus.ACCEPTED, PermissionProcessStatus.UNFULFILLABLE),
                Arguments.of(PermissionProcessStatus.ACCEPTED, PermissionProcessStatus.FULFILLED),
                Arguments.of(PermissionProcessStatus.ACCEPTED, PermissionProcessStatus.TERMINATED),
                Arguments.of(PermissionProcessStatus.UNFULFILLABLE,
                             PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION),
                Arguments.of(PermissionProcessStatus.TERMINATED, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION),
                Arguments.of(PermissionProcessStatus.FULFILLED, PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION),
                Arguments.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION,
                             PermissionProcessStatus.EXTERNALLY_TERMINATED),
                Arguments.of(PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION,
                             PermissionProcessStatus.FAILED_TO_TERMINATE),
                Arguments.of(PermissionProcessStatus.FAILED_TO_TERMINATE,
                             PermissionProcessStatus.REQUIRES_EXTERNAL_TERMINATION)
        );
    }

    public Stream<Arguments> testConstraint_onValidatedHistoricalDataStep_returnsOk() {
        var vhdStep = getValidatedHistoricalDataStep();
        var statusChangeStep = new StatusChangeStep(PermissionProcessStatus.FULFILLED, 0);
        return Stream.of(
                Arguments.of(vhdStep, statusChangeStep),
                Arguments.of(statusChangeStep, vhdStep),
                Arguments.of(vhdStep, vhdStep)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testConstraint_onFollowUpState_returnsOk(
            PermissionProcessStatus currentStatus,
            PermissionProcessStatus nextStatus
    ) {
        // Given
        var current = new StatusChangeStep(currentStatus, 0);
        var next = new StatusChangeStep(nextStatus, 0);
        var constraint = new PermissionProcessModelConstraint();

        // When
        var res = constraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    @Test
    void testConstraint_onInvalidOrder_returnsViolation() {
        // Given
        var current = new StatusChangeStep(PermissionProcessStatus.VALIDATED, 0);
        var next = new StatusChangeStep(PermissionProcessStatus.CREATED, 0);
        var constraint = new PermissionProcessModelConstraint();

        // When
        var res = constraint.violatesConstraint(current, next);

        // Then
        var violation = assertInstanceOf(ConstraintViolation.class, res);
        assertEquals("Status VALIDATED is not an allowed next status for CREATED", violation.message());
    }

    @ParameterizedTest
    @MethodSource
    void testConstraint_onValidatedHistoricalDataStep_returnsOk(Model current, Model next) {
        // Given
        var constraint = new PermissionProcessModelConstraint();

        // When
        var res = constraint.violatesConstraint(current, next);

        // Then
        assertInstanceOf(ConstraintOk.class, res);
    }

    private static ValidatedHistoricalDataStep getValidatedHistoricalDataStep() {
        return new ValidatedHistoricalDataStep(new SimulatedValidatedHistoricalData(
                "mid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        ));
    }
}