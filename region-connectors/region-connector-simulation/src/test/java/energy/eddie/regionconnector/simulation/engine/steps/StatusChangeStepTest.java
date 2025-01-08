package energy.eddie.regionconnector.simulation.engine.steps;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.DelayStep;
import energy.eddie.regionconnector.simulation.engine.steps.runtime.StatusEmissionStep;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusChangeStepTest {
    @Test
    void testExecute_returnsEmissionAndDelaySteps() {
        // Given
        var expected = List.of(
                new StatusEmissionStep(PermissionProcessStatus.CREATED),
                new DelayStep(0, ChronoUnit.SECONDS)
        );
        var step = new StatusChangeStep(PermissionProcessStatus.CREATED);
        var ctx = TestSimulationContext.create();

        // When
        var res = step.execute(ctx);

        // Then
        assertEquals(expected, res);
    }
}