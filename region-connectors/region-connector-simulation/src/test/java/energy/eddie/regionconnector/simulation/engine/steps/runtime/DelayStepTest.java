package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DelayStepTest {
    @Mock
    private DelayStep.Sleeper sleeper;

    @Test
    void testExecute_callsSleeper() throws InterruptedException {
        // Given
        var ctx = TestSimulationContext.create();
        var step = new DelayStep(10, ChronoUnit.SECONDS, sleeper);

        // When
        step.execute(ctx);

        // Then
        verify(sleeper).sleep(Duration.of(10, ChronoUnit.SECONDS));
    }

    @Test
    void testExecute_withZeroDelay_doesNotCallSleeper() throws InterruptedException {
        // Given
        var ctx = TestSimulationContext.create();
        var step = new DelayStep(0, ChronoUnit.SECONDS, sleeper);

        // When
        step.execute(ctx);

        // Then
        verify(sleeper, never()).sleep(any());
    }

    @Test
    void testEquals_forSameStep_returnsTrue() {
        // Given
        var step1 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);
        var step2 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);

        // When
        var res = step1.equals(step2);

        // Then
        assertTrue(res);
    }

    @Test
    void testHashcode_forSameStep_returnsEqualHashcode() {
        // Given
        var step1 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);
        var step2 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertEquals(res1, res2);
    }

    @Test
    void testEquals_forDifferentSteps_returnsFalse() {
        // Given
        var step1 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);
        var step2 = new DelayStep(20, ChronoUnit.SECONDS, sleeper);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }

    @Test
    void testHashcode_forDifferentSteps_returnsUnequalHashcode() {
        // Given
        var step1 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);
        var step2 = new DelayStep(20, ChronoUnit.SECONDS, sleeper);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertNotEquals(res1, res2);
    }

    @SuppressWarnings({"EqualsBetweenInconvertibleTypes", "EqualsIncompatibleType"})
    @Test
    void testEquals_forDifferentTypesOfSteps_returnsFalse() {
        // Given
        var step1 = new DelayStep(10, ChronoUnit.SECONDS, sleeper);
        var step2 = new StatusChangeStep(PermissionProcessStatus.CREATED);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }
}