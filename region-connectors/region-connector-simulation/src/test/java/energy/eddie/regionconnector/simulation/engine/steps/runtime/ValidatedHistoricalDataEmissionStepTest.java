package energy.eddie.regionconnector.simulation.engine.steps.runtime;

import energy.eddie.api.v0.PermissionProcessStatus;
import energy.eddie.regionconnector.simulation.dtos.SimulatedValidatedHistoricalData;
import energy.eddie.regionconnector.simulation.engine.steps.StatusChangeStep;
import energy.eddie.regionconnector.simulation.engine.steps.TestSimulationContext;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidatedHistoricalDataEmissionStepTest {
    @Test
    void testExecute_emitsValidatedHistoricalDataMarketDocument() {
        // Given
        var ctx = TestSimulationContext.create();
        var streams = ctx.documentStreams();
        var step = new ValidatedHistoricalDataEmissionStep(
                new SimulatedValidatedHistoricalData(
                        "mid",
                        ZonedDateTime.now(ZoneOffset.UTC),
                        "PT15M",
                        List.of()
                )
        );

        // When
        step.execute(ctx);

        // Then
        StepVerifier.create(streams.getValidatedHistoricalDataMarketDocumentsStream())
                    .then(streams::close)
                    .expectNextCount(1)
                    .verifyComplete();
    }

    @Test
    void testEquals_forSameStep_returnsTrue() {
        // Given
        var data = new SimulatedValidatedHistoricalData(
                "mid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var step1 = new ValidatedHistoricalDataEmissionStep(data);
        var step2 = new ValidatedHistoricalDataEmissionStep(data);

        // When
        var res = step1.equals(step2);

        // Then
        assertTrue(res);
    }

    @Test
    void testHashcode_forSameStep_returnsEqualHashcode() {
        // Given
        var data = new SimulatedValidatedHistoricalData(
                "mid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var step1 = new ValidatedHistoricalDataEmissionStep(data);
        var step2 = new ValidatedHistoricalDataEmissionStep(data);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertEquals(res1, res2);
    }

    @Test
    void testEquals_forDifferentSteps_returnsFalse() {
        // Given
        var data1 = new SimulatedValidatedHistoricalData(
                "mid1",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var data2 = new SimulatedValidatedHistoricalData(
                "mid2",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var step1 = new ValidatedHistoricalDataEmissionStep(data1);
        var step2 = new ValidatedHistoricalDataEmissionStep(data2);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }

    @Test
    void testHashcode_forDifferentSteps_returnsUnequalHashcode() {
        // Given
        var data1 = new SimulatedValidatedHistoricalData(
                "mid1",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var data2 = new SimulatedValidatedHistoricalData(
                "mid2",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var step1 = new ValidatedHistoricalDataEmissionStep(data1);
        var step2 = new ValidatedHistoricalDataEmissionStep(data2);

        // When
        var res1 = step1.hashCode();
        var res2 = step2.hashCode();

        // Then
        assertNotEquals(res1, res2);
    }

    @SuppressWarnings("EqualsBetweenInconvertibleTypes")
    @Test
    void testEquals_forDifferentTypesOfSteps_returnsFalse() {
        // Given
        var data = new SimulatedValidatedHistoricalData(
                "mid",
                ZonedDateTime.now(ZoneOffset.UTC),
                "PT15M",
                List.of()
        );
        var step1 = new ValidatedHistoricalDataEmissionStep(data);
        var step2 = new StatusChangeStep(PermissionProcessStatus.CREATED);

        // When
        var res = step1.equals(step2);

        // Then
        assertFalse(res);
    }
}